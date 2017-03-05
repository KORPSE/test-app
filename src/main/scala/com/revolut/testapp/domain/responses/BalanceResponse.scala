package com.revolut.testapp.domain.responses

import com.revolut.testapp.domain.model.Currency

case class BalanceResponse(currency: Currency, value: BigDecimal)
