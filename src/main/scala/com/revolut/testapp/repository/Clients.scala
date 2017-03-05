package com.revolut.testapp.repository

import com.revolut.testapp.domain.model.{Client, ClientId}
import org.slf4j.LoggerFactory
import scalikejdbc._

import scala.concurrent.{ExecutionContext, Future}

class Clients(implicit ec: ExecutionContext) {

  private val log = LoggerFactory.getLogger(getClass)

  def create(firstName: String, lastName: String): Future[Client] =
    Future {
      log.info(s"Creating client $firstName $lastName...")
      DB localTx { implicit session =>
        val c = Client.column
        val id = ClientId(
          withSQL {
            insert.into(Client).columns(c.firstName, c.lastName).values(firstName, lastName)
          }.updateAndReturnGeneratedKey.apply()
        )
        Client(id, firstName, lastName)
      }
    }

  def get(id: ClientId): Future[Option[Client]] =
    Future {
      log.info(s"Getting client by $id...")
      DB localTx { implicit session =>
        val c = Client.syntax("c")
        withSQL {
          selectFrom(Client as c)
            .where.eq(c.id, id.id)
        }.map(Client(c.resultName)).headOption().apply()
      }
    }
}
