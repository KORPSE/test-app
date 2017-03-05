package com.revolut.testapp.json

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import com.revolut.testapp.domain.model._
import com.revolut.testapp.domain.requests.{AddClientRequest, CommitTransactionRequest, TopUpRequest}
import com.revolut.testapp.domain.responses.{BalanceResponse, ErrorResponse}
import spray.json._

object JsonProtocol extends DefaultJsonProtocol {

  implicit val clientIdFormat = new JsonFormat[ClientId] {
    override def read(json: JsValue): ClientId =
      json match {
        case JsNumber(value) => ClientId(value.toLongExact)
        case x => serializationError(s"Expected JsNumber, but got $x")
      }
    override def write(obj: ClientId): JsValue = JsNumber(obj.id)
  }

  implicit val clientFormat: RootJsonFormat[Client] = jsonFormat3(Client.apply)

  implicit val chainIdFormat = new JsonFormat[TransactionChainId] {
    override def read(json: JsValue): TransactionChainId =
      json match {
        case JsString(value) => TransactionChainId(UUID.fromString(value))
        case x => serializationError(s"Expected JsString, but got $x")
      }
    override def write(obj: TransactionChainId): JsValue = JsString(obj.id.toString)
  }

  implicit val currencyFormat = new JsonFormat[Currency] {
    override def read(json: JsValue): Currency =
      json match {
        case JsString(value) => Currency(value)
        case x => serializationError(s"Expected JsString, but got $x")
      }
    override def write(obj: Currency): JsValue = JsString(obj.id)
  }

  implicit val dateFormat = new JsonFormat[LocalDateTime] {
    val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    override def read(json: JsValue): LocalDateTime =
      json match {
        case JsString(value) => LocalDateTime.parse(value, dateFormat)
        case x => serializationError(s"Expected JsString, but got $x")
      }
    override def write(obj: LocalDateTime): JsValue = JsString(obj.format(dateFormat))
  }

  implicit val transactionFormat: RootJsonFormat[Transaction] = jsonFormat6(Transaction.apply)

  implicit val addClientRequestFormat: RootJsonFormat[AddClientRequest] = jsonFormat2(AddClientRequest)

  implicit val commitTransactionRequestFormat: RootJsonFormat[CommitTransactionRequest] =
    jsonFormat4(CommitTransactionRequest)

  implicit val balanceResponseFormat: RootJsonFormat[BalanceResponse] = jsonFormat2(BalanceResponse)

  implicit val topUpRequestFormat: RootJsonFormat[TopUpRequest] = jsonFormat3(TopUpRequest)

  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse)
}
