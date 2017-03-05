package com.revolut.testapp.repository

import java.time.{LocalDateTime, ZoneOffset}

import com.revolut.testapp.domain.model._
import com.revolut.testapp.exception.{ClientNotFound, IncorrectAmount, NotEnoughFunds}
import org.slf4j.LoggerFactory
import scalikejdbc._

import scala.concurrent.{ExecutionContext, Future}

class Transactions(implicit ec: ExecutionContext) {

  private val log = LoggerFactory.getLogger(getClass)

  private val robotId = ClientId(-1l)

  def getBalance(id: ClientId, currency: Currency): Future[BigDecimal] =
    Future {
      log.info(s"Getting balance for $id...")
      DB readOnly { implicit session =>
        selectBalance(id, currency)
      }
    }

  def getClientTransactions(id: ClientId): Future[Seq[Transaction]] =
    Future {
      log.info(s"Getting transaction list for $id...")
      DB readOnly { implicit session =>
        val t = Transaction.syntax("t")
        withSQL {
          selectFrom(Transaction as t).where.eq(t.clientId, id.id).orderBy(t.dateTime)
        }.map(Transaction(t.resultName)).list().apply()
      }
    }

  def topUp(id: ClientId, currency: Currency, amount: BigDecimal): Future[BigDecimal] =
    Future {
      if (amount < 0) throw IncorrectAmount(amount, currency)
      log.info(s"Top up $amount $currency for $id...")
      DB localTx { implicit session =>
        checkClient(id)
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val balance = selectBalance(id, currency, locking = true)
        val newBalance = balance + amount
        val chainId = TransactionChainId.random()
        insertTransaction(Transaction(chainId, robotId, currency, -amount, id, now))
        insertTransaction(Transaction(chainId, id, currency, amount, robotId, now))
        updateBalance(id, currency, newBalance)
        newBalance
      }
    }

  def commit(payer: ClientId, peer: ClientId, currency: Currency, amount: BigDecimal): Future[BigDecimal] =
    Future {
      if (amount < 0) throw IncorrectAmount(amount, currency)
      log.info(s"Trying to commit transaction chain $payer -> $peer: $amount $currency")
      if (amount < 0)
        throw new IllegalArgumentException("amount should be greater than zero")

      DB localTx { implicit session =>
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val payerBalance = selectBalance(payer, currency, locking = true)
        if (payerBalance >= amount) {
          checkClient(peer)
          val chainId = TransactionChainId.random()
          val updatedPayerBalance = payerBalance - amount
          insertTransaction(Transaction(chainId, payer, currency, -amount, peer, now))
          updateBalance(payer, currency, updatedPayerBalance)
          insertTransaction(Transaction(chainId, peer, currency, amount, payer, now))
          val peerBalance = selectBalance(peer, currency, locking = true)
          updateBalance(peer, currency, peerBalance + amount)
          log.info(s"Transaction chain $payer -> $peer: $amount $currency has been successfully committed")
          updatedPayerBalance
        } else {
          log.info(s"Client with $payer has not enough balance to commit transaction chain " +
            s"$payer -> $peer: $amount $currency")
          throw NotEnoughFunds(payer, currency)
        }
      }
    }

  private def checkClient(id: ClientId)
                         (implicit session: DBSession): Unit = {
    val c = Client.syntax("c")
    withSQL {
      select(c.id).from(Client as c).where.eq(c.id, id.id)
    }.map(_.long(1)).single().apply() match {
      case Some(_) =>
      case None => throw ClientNotFound(id)
    }
  }

  private def insertTransaction(tx: Transaction)
                               (implicit session: DBSession) = {
    val t = Transaction.column
    withSQL {
      insert.into(Transaction).columns(t.id, t.clientId, t.currency, t.amount, t.peer, t.dateTime)
        .values(tx.id.id, tx.clientId.id, tx.currency.id, tx.amount, tx.peer.id, tx.dateTime)
    }.executeUpdate().apply()
  }

  private def updateBalance(clientId: ClientId, currency: Currency, amount: BigDecimal)
                           (implicit session: DBSession): Unit = {

    sql"""merge into ClientBalance (client_id, amount, currency) key (client_id, currency)
         |select ${clientId.id}, ${amount.setScale(2)}, ${currency.id} from dual"""
        .stripMargin
        .executeUpdate().apply()
  }

  private def selectBalance(id: ClientId, currency: Currency, locking: Boolean = false)
                           (implicit session: DBSession) =
    (if (locking)
      sql"""select amount from ClientBalance where client_id = ${id.id} and currency = ${currency.id} for update"""
    else
      sql"""select amount from ClientBalance where client_id = ${id.id} and currency = ${currency.id}""")
      .map(_.bigDecimal("amount")).single().apply()
      .map(BigDecimal(_)).getOrElse(BigDecimal(0).setScale(2))
}
