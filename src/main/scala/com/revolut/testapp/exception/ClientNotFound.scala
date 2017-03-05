package com.revolut.testapp.exception

import com.revolut.testapp.domain.model.ClientId

case class ClientNotFound(clientId: ClientId) extends RuntimeException {
  override def getMessage(): String = s"Client was not found in system: $clientId"
}