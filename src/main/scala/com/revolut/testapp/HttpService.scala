package com.revolut.testapp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler}
import akka.stream.ActorMaterializer
import com.revolut.testapp.domain.model.{ClientId, Currency}
import com.revolut.testapp.domain.requests.{AddClientRequest, CommitTransactionRequest, TopUpRequest}
import com.revolut.testapp.domain.responses.{BalanceResponse, ErrorResponse}
import com.revolut.testapp.json.JsonProtocol._
import com.revolut.testapp.repository.{Clients, Transactions}
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Success

class HttpService(clients: Clients, transactions: Transactions, port: Int)
                 (implicit system: ActorSystem, mat: ActorMaterializer) {

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  private def rejectionHandler = RejectionHandler.default.mapRejectionResponse { resp =>
    resp.entity match {
      case HttpEntity.Strict(ContentTypes.`text/plain(UTF-8)`, data) =>
        resp.withEntity(HttpEntity(ContentTypes.`application/json`, ErrorResponse(data.utf8String).toJson.compactPrint))
      case _ => resp
    }
  }

  private def exceptionHandler = ExceptionHandler {
    case ex: Throwable =>
      complete(StatusCodes.BadRequest -> ErrorResponse(ex.getMessage))
  }

  private val route =
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        get {
          path("client" / LongNumber) { id =>
            val clientId = ClientId(id)
            onComplete(clients.get(clientId)) {
              case Success(Some(client)) => complete(client)
              case _ => complete(StatusCodes.NotFound -> ErrorResponse(s"Client $clientId was not found"))
            }
          } ~
          path("client" / LongNumber / "balance" / Segment) { (id, cur) =>
            val currency = Currency(cur)
            complete(
              transactions.getBalance(ClientId(id), currency) map { balance =>
                BalanceResponse(currency, balance)
              }
            )
          } ~
          path("transaction" / LongNumber) { id =>
            val clientId = ClientId(id)
            complete(transactions.getClientTransactions(clientId))
          }
        } ~
        post {
          path("client") {
            entity(as[AddClientRequest]) { request =>
              complete(clients.create(request.firstName, request.lastName))
            }
          } ~
          path("client" / "topUp") {
            entity(as[TopUpRequest]) { request =>
              complete(transactions.topUp(request.clientId, request.currency, request.amount) map { balance =>
                BalanceResponse(request.currency, balance)
              })
            }
          } ~
          path("transaction") {
            entity(as[CommitTransactionRequest]) { request =>
              complete(
                transactions.commit(request.payer, request.peer, request.currency, request.amount) map { balance =>
                  BalanceResponse(request.currency, balance)
                }
              )
            }
          }
        }
      }
    }

  def serve(): Future[ServerBinding] = {
    Http().bindAndHandle(route, "0.0.0.0", port)
  }

}
