package com.revolut.testapp.domain.requests

import com.revolut.testapp.domain.model.{ClientId, Currency}

case class CommitTransactionRequest(payer: ClientId, peer: ClientId, currency: Currency, amount: BigDecimal)
