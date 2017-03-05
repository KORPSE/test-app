lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.revolut",
      scalaVersion := "2.12.1",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "revolut-test-app",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-core" % "10.0.4",
      "com.typesafe.akka" %% "akka-http" % "10.0.4",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.4",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.17",
      "org.scalikejdbc" %% "scalikejdbc" % "2.5.0",
      "org.scalikejdbc" %% "scalikejdbc-jsr310" % "2.5.0",
      "org.scalikejdbc" %% "scalikejdbc-config" % "2.5.0",
      "com.h2database" % "h2" % "1.4.193",
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "org.scalatest" %% "scalatest" % "3.0.1" % Test
    ),
    scalacOptions ++= Seq("-language:postfixOps"),
    parallelExecution in Test := false,
    test in assembly := {}
  )
