package com.kushtal.server.database

import com.kushtal.model._
import scalikejdbc._

object Account extends SQLSyntaxSupport[Account] {
  override val tableName = "account"
  override val schemaName = Some("public")
}

object AccountBalance extends SQLSyntaxSupport[AccountBalance] {
  override val tableName = "accountbalance"
  override val schemaName = Some("public")
}

object Requests {
  private val a = Account.syntax("a")
  private val b = AccountBalance.syntax("b")

  private val fromAccountLeftJoinBalance: SQLSyntax = {
    sqls"""from ${Account as a} left join ${AccountBalance as b}
           on ${a.accountid} = ${b.accountbalanceid}"""
  }

  private def whereAnd(account: Account): SQLSyntax = {
    sqls"""where ${a.userinfoid} = ${account.userinfoid}
           and ${a.currency} = ${account.currency}
           and ${a.accounttype} = ${account.accounttype}"""
  }


  /* ********** DATABASE REQUESTS ********** */

  def selectBalanceFact(account: Account)(implicit session: DBSession): Option[BigDecimal] = {
    sql"""select ${b.balancefact}
          $fromAccountLeftJoinBalance
          ${whereAnd(account)}""".
      map(_.get[math.BigDecimal](1)).single.apply()
  }

  def selectBalances(userInfoId: Long)(implicit session: DBSession): List[AccountBalanceOutput] = {
    sql"""select
            ${a.result.currency},
            ${a.result.accounttype},
            ${b.result.balancefact}
          $fromAccountLeftJoinBalance
            where
          ${a.userinfoid} = $userInfoId"""
      .map { rs =>
        AccountBalanceOutput(
          rs.string(a.resultName.currency),
          rs.string(a.resultName.accounttype),
          rs.get[math.BigDecimal](b.resultName.balancefact)
        )
      }.list.apply()
  }

  def updateBalance(accountBalanceId: Long, amount: BigDecimal)
                   (implicit session: DBSession): Option[(BigDecimal, BigDecimal)] = {
    val c = AccountBalance.column

    sql"""update
            ${AccountBalance.table}
          set
            ${c.balancefact} = ${c.balancefact} + $amount,
            ${c.balanceplan} = ${c.balanceplan} + $amount,
            ${c.balancedate} = current_timestamp
          where
            ${c.accountbalanceid} = $accountBalanceId
          returning
            ${c.balancefact}, ${c.balanceplan}"""
      .map(rs =>
        (rs.get[math.BigDecimal](1), rs.get[math.BigDecimal](2))
      ).single.apply()
  }

  def selectAccountId(account: Account)(implicit session: DBSession): Option[Long] = {
    sql"""select ${a.accountid}
          from ${Account as a}
          ${whereAnd(account)}"""
      .map(_.long(1)).single.apply()
  }

  def selectLastAccount()(implicit session: DBSession): Option[(Long, String)] = {
    sql"""select ${a.accountid}, ${a.account}
          from ${Account as a}
          order by ${a.accountid} desc
          limit 1"""
      .map(rs => (rs.long(1), rs.string(2))).single.apply()
  }

  def insertAccount(account: Account)(implicit session: DBSession): Option[Long] = {
    val c = Account.column
    val a = account
    sql"""insert into ${Account.table}
            (${c.accountid}, ${c.account}, ${c.name}, ${c.accounttype}, ${c.currency},
            ${c.blocked}, ${c.closed}, ${c.createdate}, ${c.closedate},
            ${c.comment}, ${c.userinfoid}, ${c.version}, ${c.restriction})
          values
            (${a.accountid}, ${a.account}, ${a.name}, ${a.accounttype}, ${a.currency},
            ${a.blocked}, ${a.closed}, current_timestamp, ${a.closedate},
            ${a.comment}, ${a.userinfoid}, ${a.version}, ${a.restriction})
          returning
            ${c.accountid}"""
      .map(_.long(1)).single.apply()
  }

  def insertAccountBalance(accountBalance: AccountBalance)(implicit session: DBSession): Option[Long] = {
    val c = AccountBalance.column
    val b = accountBalance
    sql"""insert into ${AccountBalance.table}
            (${c.accountbalanceid}, ${c.balancefact}, ${c.balanceplan},
            ${c.balancedate}, ${c.version})
          values
            (${b.accountbalanceid}, ${b.balancefact}, ${b.balanceplan},
            current_timestamp, ${b.version})
          returning
            ${c.accountbalanceid}"""
      .map(_.long(1)).single.apply()
  }

}
