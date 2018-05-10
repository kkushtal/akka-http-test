package com.kushtal.client

import com.kushtal.model.JsonSupport
import java.time._
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import scala.concurrent.{ExecutionContext, Future}


object Main extends App with Requests with JsonSupport {
  implicit val system: ActorSystem = ActorSystem("akka-http-client")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext = system.dispatchers.lookup("default-dispatcher")


  val (minUserInfoId, maxUserInfoId) = (1, 900)
  val currencies = Seq("USD", "RUB", "EUR")
  val accountTypes = Seq("INTERNAL", "TRADE", "TAX")

  val threadsCount = 40
  val reqsCount = 1000

  val printSuccess = true
  val printFailure = true


  /* ********** RUN CLIENT ********** */

  def initReqsLeft(): AtomicInteger = new AtomicInteger(threadsCount * reqsCount)

  println(s"Start Client\nPress RETURN to stop...")
  runClient()

  io.StdIn.readLine()
  System.exit(1)

  def runClient(): Unit = {
    val pool = Executors.newCachedThreadPool()
    val ec = ExecutionContext.fromExecutor(pool)

    val reqsLeft = initReqsLeft()
    val timeStart = Instant.now()

    Future.traverse(1 to threadsCount) { numberThread =>
      Future {
        var numberLeft = 0
        implicit val connectionFlow = initConnectionFlow()
        while ( {
          numberLeft = reqsLeft.getAndDecrement()
          numberLeft >= 0
        }) {
          val number = s"$numberThread/$numberLeft"
          math.random match {
            case v if v < .5 => getBalanceSendAwait(number)
            case _ => postTrasactSendAwait(number)
          }
        }
      }(ec)
    } onComplete (_ => println(s"Requsts Per Second: ${getRPS(timeStart)}"))
  }

  def initConnectionFlow() = Http().outgoingConnection(host = "localhost", port = 8080)

  def getRPS(timeStart: Instant): Long = {
    val timeStop = Instant.now()
    val diff = java.time.Duration.between(timeStart, timeStop)
    val count = threadsCount * reqsCount
    util.Try(count / diff.getSeconds).getOrElse(count)
  }

}