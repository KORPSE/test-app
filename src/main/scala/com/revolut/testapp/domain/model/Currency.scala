package com.revolut.testapp.domain.model

sealed abstract class Currency(val id: String)

object Currency {
  case object USD extends Currency("USD")
  case object EUR extends Currency("EUR")
  case object GBP extends Currency("GBP")

  def apply(id: String): Currency =
    id match {
      case USD.`id` => USD
      case EUR.`id` => EUR
      case GBP.`id` => GBP
    }
}
