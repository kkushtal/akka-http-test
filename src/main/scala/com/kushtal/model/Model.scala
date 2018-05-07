package com.kushtal.model

import java.time.ZonedDateTime
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol._


case class Account(accountid: Option[Long] = None, //*
                   account: Option[String] = None, //*
                   name: Option[String] = None, //*
                   accounttype: Option[String] = None,
                   currency: Option[String] = None,
                   blocked: Option[Boolean] = None,
                   closed: Option[Boolean] = None,
                   createdate: Option[ZonedDateTime] = None,
                   closedate: Option[ZonedDateTime] = None,
                   comment: Option[String] = None,
                   userinfoid: Option[Long] = None, //*
                   version: Option[Int] = None,
                   restriction: Option[String] = None,
                  )

case class AccountBalance(accountbalanceid: Long,
                          balancefact: BigDecimal,
                          balanceplan: BigDecimal,
                          balancedate: Option[ZonedDateTime] = None,
                          version: Option[Int] = None,
                         )

case class AccountBalanceOutput(Currency: String,
                                AccountType: String,
                                Balance: BigDecimal,
                               )

case class Transact(UserInfoIdFrom: Long,
                    AccountTypeFrom: String,
                    UserInfoIdTo: Long,
                    AccountTypeTo: String,
                    Currency: String,
                    Amount: BigDecimal,
                    DocumentId: Option[String] = None,
                   )

case class Error(error: Boolean,
                 errorCode: Option[String] = None,
                 errorText: Option[String] = None
                ) extends Exception


/* ********** ERRORS ********** */

object Errors {

  def wrongBalance(accountId: Long): Error = Error(
    error = true,
    errorCode = Some("0"),
    errorText = Some(s"Wrong balance; [AccountBalanceId: $accountId]")
  )

  def balanceNotFound(accountId: Long): Error = Error(
    error = true,
    errorCode = Some("1"),
    errorText = Some(s"Balance not found; [AccountBalanceId: $accountId]")
  )

  def accountNotFound(account: Account): Error = Error(
    error = true,
    errorCode = Some("2"),
    errorText = Some(s"Account not found; [UserInfoId: ${account.userinfoid.get}]")
  )

  def canNotBeCreated(account: Account): Error = Error(
    error = true,
    errorCode = Some("3"),
    errorText = Some(s"Account not found; [UserInfoId: ${account.userinfoid.get}]\n" +
      s"Account can not be created [AccountId: ${account.accountid.get}"
    )
  )
}


/* ********** CREATORS ********** */

object Create {
  def balance(accountId: Long): AccountBalance = AccountBalance(
    accountbalanceid = accountId,
    balancefact = 0,
    balanceplan = 0,
    version = Some(0)
  )

  def account(userInfoId: Long, currency: String, accountType: String): Account = Account(
    userinfoid = Some(userInfoId),
    currency = Some(currency),
    accounttype = Some(accountType),
  )

  def account(inAccount: Account, accountId: Long, account: String): Account = inAccount.copy(
    accountid = Some(accountId),
    account = Some(account),
    name = inAccount.currency,
    comment = Some("Automatically created account"),
    version = Some(0)
  )
}


/* ********** JSON SUPPORT ********** */

trait JsonSupport extends SprayJsonSupport {
  implicit val accountBalanceOuputJsonFormat = jsonFormat3(AccountBalanceOutput)
  implicit val transactJsonFormat = jsonFormat7(Transact)
  implicit val errorJsonFormat = jsonFormat3(Error)
}