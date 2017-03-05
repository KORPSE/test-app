package com.revolut.testapp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import com.revolut.testapp.repository.{Clients, Transactions}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Service {
  def start(): Future[ServerBinding] = {
    implicit val system = ActorSystem("TestAppSystem")
    implicit val mat = ActorMaterializer()

    val (clients, transactions) = {
      implicit val blockingEc = system.dispatchers.lookup("blocking-dispatcher")
      (new Clients(), new Transactions())
    }

    implicit val ec = system.dispatcher
    val httpService = new HttpService(clients, transactions, 8080)

    httpService.serve().recover {
      case t: Throwable =>
        system.log.error(s"Fatal exception: $t")
        Await.result(
          for {
            _ <- Http().shutdownAllConnectionPools()
            _ <- system.terminate()
          } yield (), 10 seconds)
        throw t
    }
  }
}
