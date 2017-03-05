package com.revolut.testapp.repository

import com.revolut.testapp.CommonTest
import com.revolut.testapp.domain.model.{ClientId, Currency}
import com.revolut.testapp.exception.{IncorrectAmount, NotEnoughFunds}
import org.scalatest.BeforeAndAfter
import scalikejdbc._

import scala.concurrent.ExecutionContext.Implicits.global

class TransactionsTest extends CommonTest with BeforeAndAfter {

  val transactions = new Transactions()
  val johnId = ClientId(1)
  val richardId = ClientId(2)

  private def setBalances(john: Option[BigDecimal], richard: Option[BigDecimal], currency: Currency) =
    DB localTx { implicit session =>
      john.map((johnId, _)) ++ richard.map((richardId, _)) foreach { case (id, amount) =>
        sql"insert into ClientBalance (client_id, currency, amount) values (${id.id}, ${currency.id}, $amount);".executeUpdate().apply()
      }
    }

  before {
    DB localTx { implicit session =>
      sql"truncate table Client; truncate table ClientBalance;".executeUpdate().apply()
      sql"""insert into Client (id, first_name, last_name)
           |values (${johnId.id}, 'John', 'Doe'), (${richardId.id}, 'Richard', 'Roe');""".stripMargin.executeUpdate().apply()
    }
  }

  it should "get balance" in {
    setBalances(john = Some(BigDecimal("100")), richard = None, currency = Currency.USD)
    whenReady(transactions.getBalance(johnId, Currency.USD)) { richardBalance =>
      richardBalance shouldBe BigDecimal(100).setScale(2)
    }
  }

  it should "commit transactions if there are enough funds" in {
    setBalances(john = Some(BigDecimal("100")), richard = None, currency = Currency.USD)
    whenReady(
      for {
        _ <- transactions.commit(johnId, richardId, Currency.USD, BigDecimal(50))
        rb <- transactions.getBalance(richardId, Currency.USD)
        jb <- transactions.getBalance(johnId, Currency.USD)
      } yield (rb, jb)) {
      case (richardBalance, johnBalance) =>
        richardBalance shouldBe BigDecimal("50.00")
        johnBalance shouldBe BigDecimal("50.00")
    }
  }

  it should "return an error if there are not enough funds" in {
    setBalances(john = Some(BigDecimal("40")), richard = None, currency = Currency.USD)
    whenReady(transactions.commit(johnId, richardId, Currency.USD, BigDecimal(50)).failed) { ex =>
      ex shouldBe NotEnoughFunds(johnId, Currency.USD)
    }
  }

  it should "return an error on commit if amount < 0" in {
    whenReady(transactions.commit(johnId, richardId, Currency.USD, BigDecimal(-50)).failed) { ex =>
      ex shouldBe IncorrectAmount(BigDecimal(-50), Currency.USD)
    }
  }

  it should "return an error on top up if amount < 0" in {
    whenReady(transactions.topUp(johnId, Currency.USD, BigDecimal(-50)).failed) { ex =>
      ex shouldBe IncorrectAmount(BigDecimal(-50), Currency.USD)
    }
  }

}
