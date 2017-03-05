package com.revolut.testapp.domain.requests

import com.revolut.testapp.domain.model.{ClientId, Currency}

case class TopUpRequest(clientId: ClientId, amount: BigDecimal, currency: Currency)
