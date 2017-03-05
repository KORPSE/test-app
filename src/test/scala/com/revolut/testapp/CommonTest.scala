package com.revolut.testapp

import com.revolut.testapp.util.H2DB
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

trait CommonTest extends FlatSpec with ScalaFutures with Matchers {
  override implicit val patienceConfig = PatienceConfig(timeout = 5 second, interval = 50 millis)
  H2DB.setup()
}
