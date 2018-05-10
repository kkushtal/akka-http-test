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
  /*generate()*/

  Http().bindAndHandle(routes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")
  Await.result(system.whenTerminated, Duration.Inf)


  /*def generate() = {
    import com.kushtal._

    val currencies = Seq("USD", "RUB", "EUR")
    val accountTypes = Seq("INTERNAL", "TRADE", "TAX")
    val (userIdFrom, userIdTo) = (300, 600)

    (userIdFrom to userIdTo).map { id =>
      currencies.map { cur =>
        accountTypes.map { accType =>
          val acc = model.Account(
            userInfoId = Some(id),
            currency = Some(cur),
            accountType = Some(accType)
          )
          scalikejdbc.DB localTx { implicit session =>
            val accId = server.Database.getOrCreateAccountId(acc)
            println(s"$id: $accId")
          }
        }
      }
    }
  }*/

  /*
  alter sequence accountidseq restart with 1;
  delete from account;
  delete from accountbalance;

  update accountbalance set balancefact=10000, balanceplan=10000;
   */


}
