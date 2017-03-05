package com.revolut.testapp.repository

import com.revolut.testapp.CommonTest

import scala.concurrent.ExecutionContext.Implicits.global

class ClientsTest extends CommonTest {

  val clients = new Clients()

  it must "save new client" in {
    whenReady(clients.create("John", "Doe")) { client =>
      client.firstName shouldBe "John"
      client.lastName shouldBe "Doe"
      client.id.id should be > 0l
    }
  }

  it must "retrieve existing client" in {
    whenReady(
      for {
        client <- clients.create("John", "Doe")
        result <- clients.get(client.id)
      } yield (client, result)
    ) {
      case (existing, Some(gotten)) =>
        existing shouldEqual gotten
      case _ =>
        fail()
    }
  }

}
