package com.kushtal.server

import com.kushtal.model._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalikejdbc._


object Database {

  def getAccountId(acc: Account)(implicit session: DBSession): Long = {
    val accountId =
      sql"""select accountid
            from public.account
            where userinfoid = ${acc.userInfoId}
              and currency = ${acc.currency}
              and accounttype = ${acc.accountType}"""
        .map(_.long(1)).single.apply()

    accountId getOrElse (throw Error.accountNotFound(acc))
  }


  def getOrCreateAccountId(inAcc: Account)(implicit session: DBSession): Long = {
    val newAcc = inAcc.copy(
      account = Some("UID"),
      name = inAcc.currency,
      comment = Some("Automatically created account"),
      version = Some(0)
    )

    val newAccountId =
      sql"""insert into public.account
              (accountid, account, name, accounttype, currency, blocked, closed,
              createdate, closedate, comment, userinfoid, version, restriction)
            select
              nextval(${"accountidseq"}), ${newAcc.account}, ${newAcc.name},
              ${newAcc.accountType}, ${newAcc.currency}, ${newAcc.blocked},
              ${newAcc.closed}, current_timestamp, ${newAcc.closeDate}, ${newAcc.comment},
              ${newAcc.userInfoId}, ${newAcc.version}, ${newAcc.restriction}
            where not exists (
              select 1
              from public.account
              where userinfoid = ${newAcc.userInfoId}
                and currency = ${newAcc.currency}
                and accounttype = ${newAcc.accountType}
            )
            returning accountid"""
        .map(_.long(1)).single.apply()

    val newBalance = AccountBalance(
      accountBalanceId = newAccountId getOrElse getAccountId(inAcc)
    )

    sql"""insert into public.accountbalance
            (accountbalanceid, balancefact, balanceplan, balancedate, version)
          values
            (${newBalance.accountBalanceId}, ${newBalance.balanceFact},
            ${newBalance.balancePlan}, current_timestamp, ${newBalance.version})
          on conflict(accountbalanceid) do nothing"""
      .execute.apply()

    newBalance.accountBalanceId
  }


  private def updateBalance(accBalanceId: Long, amount: BigDecimal)(implicit session: DBSession): Unit = {
    val balances =
      sql"""update public.accountbalance
            set
              balancefact = balancefact + $amount,
              balanceplan = balanceplan + $amount,
              balancedate = current_timestamp
            where
              accountbalanceid = $accBalanceId
            returning
              balancefact, balanceplan"""
        .map(rs =>
          (rs.get[math.BigDecimal](1), rs.get[math.BigDecimal](2))
        ).single.apply()

    balances match {
      case Some((fact, plan)) if fact < 0 || plan < 0 => throw Error.wrongBalance(accBalanceId)
      case Some(_) => ()
      case _ => throw Error.balanceNotFound(accBalanceId)
    }
  }


  def getBalance(acc: Account): Future[String] = DB futureLocalTx { implicit session =>
    Future {
      val balance =
        sql"""select b.balancefact
              from public.account a left join public.accountbalance b
                on a.accountid = b.accountbalanceid
              where a.userinfoid = ${acc.userInfoId}
                and a.currency = ${acc.currency}
                and a.accounttype = ${acc.accountType}"""
          .map(_.get[math.BigDecimal](1)).single.apply()

      balance match {
        case Some(v) if v == 0 => 0.toString //“0E-8”
        case Some(v) => v.toString
        case _ => "null"
      }
    }
  }


  def getBalances(userInfoId: Long): Future[List[AccountBalanceOutput]] = DB futureLocalTx { implicit session =>
    Future {
      sql"""select a.currency, a.accounttype, b.balancefact
            from public.account a left join public.accountbalance b
              on a.accountid = b.accountbalanceid
            where a.userinfoid = $userInfoId"""
        .map { rs =>
          AccountBalanceOutput(
            rs.string("currency"),
            rs.string("accounttype"),
            rs.get[math.BigDecimal]("balancefact")
          )
        }.list.apply()
    }
  }


  def makeTransacts(transacts: List[Transact]): Future[Error] = DB futureLocalTx { implicit session =>
    Future {
      val accIds = transacts.flatMap { t =>
        val fromAccId = getAccountId(Account.transactFrom(t))
        val toAccId = getOrCreateAccountId(Account.transactTo(t))

        List(fromAccId -> t.Amount * -1, toAccId -> t.Amount)
      }.sorted //.sortBy(_._1)

      accIds.foreach { case (accId, amount) => updateBalance(accId, amount) }
      Error(error = false)
    }
  }

}

