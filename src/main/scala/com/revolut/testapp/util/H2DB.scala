package com.revolut.testapp.util

import com.typesafe.config.ConfigFactory
import scalikejdbc._
import scalikejdbc.config.DBs

import scala.io.Source

object H2DB {
  private val config = ConfigFactory.load()

  private lazy val sql = {
    val is = getClass.getClassLoader.getResourceAsStream("crebas.sql")
    Source.fromInputStream(is).mkString
  }

  def setup(): Unit = {
    DBs.loadGlobalSettings()
    ConnectionPool.singleton(
      config.getString("app.db.connectionString"),
      config.getString("app.db.user"),
      config.getString("app.db.password"))

    using(DB(ConnectionPool.borrow())) { db =>
      db.localTx { implicit session =>
        SQL(sql).executeUpdate().apply()
      }
    }
  }

}
