package com.kushtal.server.database

import com.kushtal.model._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalikejdbc._


object Database {

  private def getAccountId(acc: Account)(implicit session: DBSession): Long = {
    val accId = Requests.selectAccountId(acc)
    accId getOrElse createAccount(acc)
  }

  private def updateBalance(accountId: Long, amount: BigDecimal)(implicit session: DBSession): Unit = {
    Requests.updateBalance(accountId, amount) match {
      case Some((fact, plan)) if fact < 0 || plan < 0 => throw Errors.wrongBalance(accountId)
      case Some(_) => ()
      case _ => throw Errors.balanceNotFound(accountId)
    }
  }

  private def createAccount(inAcc: Account)(implicit session: DBSession): Long = {
    var (inAccId, inAccUid) = Requests.selectLastAccount().get
    inAccId += 1
    val acc = Create.account(inAcc, inAccId, inAccUid)

    val accId = createBalance(inAccId)
    Requests.insertAccount(acc)
    accId getOrElse(throw Errors.canNotBeCreated(acc))
  }

  private def createBalance(accountId: Long)(implicit session: DBSession): Option[Long] = {
    val balance = Create.balance(accountId)
    Requests.insertAccountBalance(balance)
  }


  /* ********** ROUTE REQUESTS ********** */

  def getBalance(account: Account): Future[String] = DB futureLocalTx { implicit session =>
    Future {
      Requests.selectBalanceFact(account).map(_.toString) getOrElse "null"
    }
  }

  def getBalances(userInfoId: Long): Future[List[AccountBalanceOutput]] = DB futureLocalTx { implicit session =>
    Future {
      Requests.selectBalances(userInfoId)
    }
  }

  def makeTransactions(transacts: List[Transact]): Future[Error] = DB futureLocalTx { implicit session =>
    Future {
      transacts.foreach { t =>
        val accountIdFrom = getAccountId(Create.account(t.UserInfoIdFrom, t.Currency, t.AccountTypeFrom))
        val accountIdTo = getAccountId(Create.account(t.UserInfoIdTo, t.Currency, t.AccountTypeTo))

        Seq(accountIdFrom -> t.Amount * -1, accountIdTo -> t.Amount)
          .sorted
          .foreach { case (accountId, amount) => updateBalance(accountId, amount) }
      }
      Error(error = false)
    }
  }
}
