package com.kushtal.client

import java.time._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


object Main extends App with Requests {
  implicit val system: ActorSystem = ActorSystem("kushtalClient")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val (minUserInfoId, maxUserInfoId) = (762, 763)
  val currencies = Seq("USD", "RUB", "EUR")
  val accountTypes = Seq("INTERNAL", "TRADE", "TAX")

  val threadCounts = 1
  val requestCounts = 5

  val printSuccess = true
  val printFailure = true


  /* ********** RUN CLIENT ********** */

  println(s"Start Client\nPress RETURN to stop...")
  runClient()

  io.StdIn.readLine()
  System.exit(1)

  def runClient(): Unit = {
    val timeStart = Instant.now()
    Future.traverse(1 to threadCounts) { threadN =>
      Future {
        (1 to requestCounts) foreach { requestN =>
          val number = requestN * threadN
          if (math.random < .5)
            sendRequestGetBalance(number)
          else
            sendRequestTransact(number)
        }
      }
    } onComplete (_ => println(s"Requsts Per Second: ${getRPS(timeStart)}"))
  }

  def getRPS(timeStart: Instant): Long = {
    val timeStop = Instant.now()
    val diff = Duration.between(timeStart, timeStop)
    util.Try(threadCounts * requestCounts / diff.getSeconds).getOrElse(1)
  }

}

/*val pool = Executors.newCachedThreadPool()
val ec = ExecutionContext.fromExecutor(pool)*/

