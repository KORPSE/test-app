package com.revolut.testapp

import com.revolut.testapp.util.H2DB

object Main extends App {
  H2DB.setup()
  Service.start()
}
