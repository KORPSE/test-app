package com.revolut.testapp.domain.model

import java.time.LocalDateTime
import java.util.UUID

import scalikejdbc._
import scalikejdbc.jsr310._

case class TransactionChainId(id: UUID)

object TransactionChainId {
  def random() = TransactionChainId(UUID.randomUUID())
}

case class Transaction(id: TransactionChainId, clientId: ClientId, currency: Currency, amount: BigDecimal, peer: ClientId, dateTime: LocalDateTime)

object Transaction extends SQLSyntaxSupport[Transaction] {

  override val tableName = "Transaction"

  def apply(o: ResultName[Transaction])(rs: WrappedResultSet): Transaction =
    Transaction(
      id = TransactionChainId(UUID.nameUUIDFromBytes(rs.bytes(o.id))),
      clientId = ClientId(rs.long(o.clientId)),
      currency = Currency(rs.string(o.currency)),
      peer = ClientId(rs.long(o.peer)),
      amount = rs.bigDecimal(o.amount),
      dateTime = rs.localDateTime(o.dateTime)
    )

}
