package com.kushtal.server

import database._
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
    pathPrefix("getbalance") {
      path(LongNumber / Segment / Segment) { case (userInfoId, currency, accountType) =>
        get {
          val account = Create.account(userInfoId, currency, accountType)
          complete(Database.getBalance(account))
        }
      }
    } ~
      pathPrefix("getbalances") {
        path(LongNumber) { userinfoid =>
          get {
            complete(Database.getBalances(userinfoid))
          }
        }
      } ~
      pathPrefix("transact") {
        pathEndOrSingleSlash {
          post {
            entity(as[List[Transact]]) { transacts =>
              onSuccess(Database.makeTransactions(transacts)) { error =>
                complete(error)
              }
            }
          }
        }
      }
  }

}
