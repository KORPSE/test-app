package com.revolut.testapp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import spray.json._
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import scalikejdbc._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class IntegrationalTest extends CommonTest with BeforeAndAfter with BeforeAndAfterAll {

  private implicit val system = ActorSystem()
  private implicit val mat = ActorMaterializer()
  private implicit val ec = system.dispatcher

  private val baseUrl = "http://127.0.0.1:8080"

  private val binding = Service.start()

  before {
    DB localTx { implicit session =>
      sql"truncate table Client; truncate table ClientBalance;".executeUpdate().apply()
    }
  }


  it should "register new client" in {
    whenReady(post("client", """{"firstName":"John","lastName":"Doe"}""".stripMargin)) { resp =>
      resp("firstName") shouldBe JsString("John")
      resp("lastName") shouldBe JsString("Doe")
      resp("id") shouldBe a [JsNumber]
    }
  }

  it should "get existing client" in {
    whenReady(
      for {
        r <- post("client", """{"firstName":"John","lastName":"Doe"}""")
        response <- get(s"client/${r("id")}")
      } yield response
    ) { resp =>
      resp("firstName") shouldBe JsString("John")
      resp("lastName") shouldBe JsString("Doe")
      resp("id") shouldBe a [JsNumber]
    }
  }

  it should "create clients, top up, send, check resulting balance, check tx list" in {
    whenReady(
      for {
        //Create John Doe
        john <- post("client", """{"firstName":"John","lastName":"Doe"}""")
        //Create Richard Roe
        richard <- post("client", """{"firstName":"Richard","lastName":"Roe"}""")
        johnId = john("id").asInstanceOf[JsNumber].value
        richardId = richard("id").asInstanceOf[JsNumber].value
        //top up 100 USD to John
        startBal <- post(s"client/topUp", s"""{"clientId":$johnId,"currency":"USD","amount":100.00}""")
        //send 40 USD from John to Richard
        endBal <- post(s"transaction", s"""{"payer":$johnId, "peer":$richardId,"currency":"USD","amount":40.00}""")
        //check result Richard Balance
        richardBal <- get(s"client/$richardId/balance/USD")
        johnTxs <- get(s"transaction/$johnId")
      } yield (startBal, endBal, richardBal, johnTxs, richardId)
    ) {
      case (start, end, peer, txs, peerId) =>
        start("value") shouldBe JsNumber("100.00")
        start("currency") shouldBe JsString("USD")
        end("value") shouldBe JsNumber("60.00")
        end("currency") shouldBe JsString("USD")
        peer("value") shouldBe JsNumber("40.00")
        txs(0)("peer") shouldBe JsNumber(-1) // robot
        txs(0)("amount") shouldBe JsNumber("100.00") // robot
        txs(1)("peer") shouldBe JsNumber(peerId)
        txs(1)("amount") shouldBe JsNumber("-40.00")
    }
  }

  it should "format error responses to json" in {
    whenReady(get("bad_route")) { response =>
      response.asJsObject.fields should contain key "error"
    }
  }

  it should "return json message if client wasn't found" in {
    whenReady(get("client/42")) { response =>
      response.asJsObject.fields should contain key "error"
    }
  }

  it should "return an error on top up nonexisting client" in {
    whenReady(post(s"client/topUp", s"""{"clientId":42,"currency":"USD","amount":100.00}""")) { response =>
      response("error") shouldBe JsString(s"Client was not found in system: ClientId(42)")
    }
  }

  it should "return an error on sending funds to nonexisting client" in {
    whenReady(
      for {
        john <- post("client", """{"firstName":"John","lastName":"Doe"}""")
        johnId = john("id").asInstanceOf[JsNumber].value
        _ <- post(s"client/topUp", s"""{"clientId":$johnId,"currency":"USD","amount":100.00}""")
        r <- post(s"transaction", s"""{"payer":$johnId, "peer":42,"currency":"USD","amount":40.00}""")
      } yield r) { response =>
      response("error") shouldBe JsString(s"Client was not found in system: ClientId(42)")
    }
  }

  override def afterAll(): Unit = {
    Await.result(
      for {
        _ <- binding
        _ <- Http().shutdownAllConnectionPools()
        _ <- system.terminate()
      } yield (),
      5 seconds)
  }

  implicit class RichJsValue(self: JsValue) {
    def apply(field: String): JsValue = self.asJsObject.fields(field)
    def apply(n: Int): JsValue = self.asInstanceOf[JsArray].elements(n)
  }

  private def get(url: String): Future[JsValue] = {
    Http().singleRequest(Get(s"$baseUrl/$url"))
      .flatMap(_.entity.toStrict(3 seconds).map(_.data.utf8String.parseJson))
  }

  private def post(url: String, payload: String): Future[JsValue] = {
    Http().singleRequest(Post(s"$baseUrl/$url", HttpEntity(ContentTypes.`application/json`, payload)))
      .flatMap(_.entity.toStrict(3 seconds).map(_.data.utf8String.parseJson))
  }
}
