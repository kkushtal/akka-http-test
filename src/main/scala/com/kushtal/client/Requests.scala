package com.kushtal.client

import com.kushtal.model._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success}
import spray.json.DefaultJsonProtocol._


trait Requests extends JsonSupport {
  implicit def system: ActorSystem

  implicit def materializer: ActorMaterializer

  val minUserInfoId: Long
  val maxUserInfoId: Long
  val currencies: Seq[String]
  val accountTypes: Seq[String]

  val printSuccess: Boolean
  val printFailure: Boolean

  private def getRandom[T](seq: Seq[T]): T = Random.shuffle(seq).last

  private def randUserInfoId: Long = getRandom(minUserInfoId to maxUserInfoId)

  private def randCurrency: String = getRandom(currencies)

  private def randAccountType: String = getRandom(accountTypes)

  private def getRandomUriGetBalance: Uri = {
    s"/getbalance/$randUserInfoId/$randCurrency/$randAccountType"
  }

  private def getRandomTransact: Transact = Transact(
    UserInfoIdFrom = randUserInfoId,
    AccountTypeFrom = randAccountType,
    UserInfoIdTo = randUserInfoId,
    AccountTypeTo = randAccountType,
    Currency = randCurrency,
    Amount = 1
  )

  private def getRandomTransacts: List[Transact] = {
    (1 to getRandom(1 to 3)).map(_ => getRandomTransact).toList
  }


  /* ********** REQUESTS ********** */

  private def sendRequest(number: Int, httpRequest: HttpRequest): Unit = {
    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnection(host = "localhost", port = 8080)

    val futureResponse = for {
      response <- Source.single(httpRequest).via(connectionFlow).runWith(Sink.head)
      entity <- Unmarshal(response.entity).to[String]
    } yield entity

    futureResponse onComplete {
      case Success(v) if printSuccess => println(s"$number: $v")
      case Failure(f) if printFailure => println(s"$number: Failed: $f")
    }
  }

  def sendRequestGetBalance(number: Int): Unit = {
    val uri = getRandomUriGetBalance
    val httpRequest = HttpRequest(uri = uri)
    sendRequest(number, httpRequest)
  }

  def sendRequestTransact(number: Int): Unit = {
    val uri: Uri = "/transact"
    val transacts = getRandomTransacts
    Marshal(transacts).to[RequestEntity] foreach {
      httpEntity =>
        val httpRequest = HttpRequest(method = HttpMethods.POST, uri = uri, entity = httpEntity)
        sendRequest(number, httpRequest)
    }
  }

}