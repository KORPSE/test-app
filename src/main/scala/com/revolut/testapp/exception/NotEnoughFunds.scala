package com.revolut.testapp.exception

import com.revolut.testapp.domain.model.{ClientId, Currency}

case class NotEnoughFunds(clientId: ClientId, currency: Currency) extends RuntimeException {
  override def getMessage: String = s"User $clientId has not enough funds for this transaction (currency: $currency)"
}
