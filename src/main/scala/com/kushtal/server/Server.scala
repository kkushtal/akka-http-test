package com.kushtal.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalikejdbc.config.DBs


object Main extends App with Routes {
  implicit val system: ActorSystem = ActorSystem("kushtalServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  DBs.setupAll()
  Http().bindAndHandle(routes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")
  Await.result(system.whenTerminated, Duration.Inf)

}

