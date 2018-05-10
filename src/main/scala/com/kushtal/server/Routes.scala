package com.kushtal.server

import com.kushtal.model._
import java.sql.SQLException
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import spray.json.DefaultJsonProtocol._

trait Routes extends JsonSupport {
  implicit def system: ActorSystem

  implicit def myExceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: Error => complete(InternalServerError -> ex)
    case ex: SQLException => complete(InternalServerError -> ex.getMessage)
  }

  val routes: Route = {
    (get & path("getbalance" / LongNumber / Segment / Segment)) { (userInfoId, currency, accountType) =>
      val account = Account.byMinimals(userInfoId, currency, accountType)
      complete(Database.getBalance(account))
    } ~
      (get & path("getbalances" / LongNumber)) { userinfoid =>
        complete(Database.getBalances(userinfoid))
      } ~
      (post & path("transact") & entity(as[List[Transact]])) { transacts =>
        onSuccess(Database.makeTransacts(transacts)) { error =>
          complete(error)
        }
      }
  }
}
