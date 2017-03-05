package com.revolut.testapp.domain.model

import scalikejdbc._

case class ClientId(id: Long)

case class Client(id: ClientId, firstName: String, lastName: String)

object Client extends SQLSyntaxSupport[Client] {

  override val tableName = "Client"

  def apply(c: ResultName[Client])(rs: WrappedResultSet): Client =
    Client(
      id = ClientId(rs.long(c.id)),
      firstName = rs.string(c.firstName),
      lastName = rs.string(c.lastName)
    )
}
