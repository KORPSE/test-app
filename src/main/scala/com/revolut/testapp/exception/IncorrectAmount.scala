package com.revolut.testapp.exception

import com.revolut.testapp.domain.model.Currency

case class IncorrectAmount(amount: BigDecimal, currency: Currency) extends RuntimeException {
  override def getMessage(): String = s"Amount should be greater than zero: $amount $currency"
}