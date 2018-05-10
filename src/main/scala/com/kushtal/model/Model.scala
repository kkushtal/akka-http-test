package com.kushtal.model

import java.time.ZonedDateTime
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol._


case class Account(accountId: Option[Long] = None, //*
                   account: Option[String] = None, //*
                   name: Option[String] = None, //*
                   accountType: Option[String] = None,
                   currency: Option[String] = None,
                   blocked: Option[Boolean] = None,
                   closed: Option[Boolean] = None,
                   createDate: Option[ZonedDateTime] = None,
                   closeDate: Option[ZonedDateTime] = None,
                   comment: Option[String] = None,
                   userInfoId: Option[Long] = None, //*
                   version: Option[Int] = None,
                   restriction: Option[String] = None,
                  )

object Account {
  def byMinimals(userInfoId: Long, currency: String, accountType: String): Account = Account(
    userInfoId = Some(userInfoId),
    currency = Some(currency),
    accountType = Some(accountType),
  )

  def transactFrom(transact: Transact): Account = byMinimals(
    transact.UserInfoIdFrom,
    transact.Currency,
    transact.AccountTypeFrom,
  )

  def transactTo(transact: Transact): Account = byMinimals(
    transact.UserInfoIdTo,
    transact.Currency,
    transact.AccountTypeTo
  )
}

case class AccountBalance(accountBalanceId: Long,
                          balanceFact: BigDecimal = 0,
                          balancePlan: BigDecimal = 0,
                          balanceDate: Option[ZonedDateTime] = None,
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

object Error {
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
    errorText = Some(s"Account not found; [UserInfoId: ${account.userInfoId.get}]")
  )

  def canNotBeCreated(account: Account): Error = Error(
    error = true,
    errorCode = Some("3"),
    errorText = Some(s"Account not found; [UserInfoId: ${account.userInfoId.get}]\n" +
      s"Account can not be created [AccountId: ${account.accountId.get}"
    )
  )
}


/* ********** JSON SUPPORT ********** */

trait JsonSupport extends SprayJsonSupport {
  implicit val accountBalanceOuputJsonFormat = jsonFormat3(AccountBalanceOutput)
  implicit val transactJsonFormat = jsonFormat7(Transact.apply)
  implicit val errorJsonFormat = jsonFormat3(Error.apply)
}