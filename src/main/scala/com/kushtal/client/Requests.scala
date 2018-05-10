package com.kushtal.client

import com.kushtal.model._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Random}
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

  def getBalanceSendAwait(number: String)(implicit connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]]): Unit = {
    val uri: Uri = getRandomUriGetBalance
    val req = HttpRequest(method = HttpMethods.GET, uri = uri)
    requestSendAwait(number, req)
  }

  def postTrasactSendAwait(number: String)(implicit connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]]): Unit = {
    val uri: Uri = "/transact"
    val transacts = getRandomTransacts

    Marshal(transacts).to[RequestEntity] map { httpEntity =>
      val req = HttpRequest(method = HttpMethods.POST, uri = uri, entity = httpEntity)
      requestSendAwait(number, req)
    }
  }

  private def requestSendAwait(number: String, request: HttpRequest)
                              (implicit connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]]): Unit = {
    val start = System.currentTimeMillis()
    val fResponse = for {
      response <- Source.single(request).via(connectionFlow).runWith(Sink.head)
      entity <- Unmarshal(response.entity).to[String]
    } yield entity

    fResponse onComplete {
      case Success(v) if printSuccess => printResponse(start, number, v)
      case Failure(f) if printFailure => printResponse(start, s"FAIL $number", f)
    }
    Await.ready(fResponse, Duration.Inf)
  }

  def printResponse(start: Long, number: String, data: Object): Unit = {
    val end = System.currentTimeMillis()
    println(s"$number: Result in ${end - start} millis: $data")
  }
}
