package it.marcopaggioro.easypay.routes

import akka.Done
import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler, SupervisorStrategy}
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.*
import akka.http.scaladsl.server.directives.BasicDirectives.extractRequest
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.pattern.StatusReply
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import cats.data.Validated
import io.circe.Encoder.encodeSeq
import io.circe.*
import io.circe.jawn.decode
import io.circe.syntax.EncoderOps
import it.marcopaggioro.easypay.AppConfig.askTimeout
import it.marcopaggioro.easypay.actor.TransactionsManagerActor
import it.marcopaggioro.easypay.database.PostgresProfile.*
import it.marcopaggioro.easypay.database.PostgresProfile.api.*
import it.marcopaggioro.easypay.database.scheduledoperations.{ScheduledOperationRecord, ScheduledOperationsTable}
import it.marcopaggioro.easypay.database.scheduledoperations.ScheduledOperationRecord.ScheduledOperationUserJoinEncoder
import it.marcopaggioro.easypay.database.transactionshistory.TransactionsHistoryRecord.TransactionUserJoinEncoder
import it.marcopaggioro.easypay.database.transactionshistory.{TransactionsHistoryRecord, TransactionsHistoryTable}
import it.marcopaggioro.easypay.database.users.UserRecord.UserRecordEncoder
import it.marcopaggioro.easypay.database.users.{UserRecord, UsersTable}
import it.marcopaggioro.easypay.database.usersbalance.{UserBalanceRecord, UsersBalanceTable}
import it.marcopaggioro.easypay.domain.TransactionsManager.TransactionsManagerCommand
import it.marcopaggioro.easypay.domain.UsersManager.UsersManagerCommand
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, ScheduledOperationId}
import it.marcopaggioro.easypay.domain.classes.userdata.{Email, UserData}
import it.marcopaggioro.easypay.domain.classes.{Money, ScheduledOperation, Validable}
import it.marcopaggioro.easypay.domain.{TransactionsManager, UsersManager}
import it.marcopaggioro.easypay.routes.EasyPayAppRoutes.{ScheduledOperationTupleEncoder, circeUnmarshaller}
import it.marcopaggioro.easypay.routes.payloads.LoginPayload.LoginPayloadDecoder
import it.marcopaggioro.easypay.routes.payloads.scheduledoperation.CreateScheduledOperationPayload
import it.marcopaggioro.easypay.routes.payloads.{LoginPayload, TransferMoneyPayload, UpdateUserDataPayload}
import it.marcopaggioro.easypay.utilities.JwtUtils
import slick.jdbc.JdbcBackend.Database
import slick.lifted.TableQuery.Extract

import java.util.UUID
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class EasyPayAppRoutes(usersManagerActorRef: ActorRef[UsersManagerCommand], database: Database)(implicit
    system: ActorSystem[Nothing]
) {

  private implicit val scheduler: Scheduler = system.scheduler
  private implicit val executionContext: ExecutionContextExecutor = system.executionContext

  private val transactionsManagerActorRef: ActorRef[TransactionsManagerCommand] = system.systemActorOf(
    Behaviors.supervise(TransactionsManagerActor()).onFailure[Exception](SupervisorStrategy.restart),
    TransactionsManagerActor.Name
  )

  private val generateJsonError: String => Json = error => Json.obj("error" -> error.asJson)

  private def completeWithJson(json: Json, statusCode: StatusCode = StatusCodes.OK): StandardRoute =
    complete(statusCode, HttpEntity(ContentTypes.`application/json`, json.noSpaces))

  private def completeWithError(statusCode: StatusCode, error: String)(implicit uri: Uri): StandardRoute = {
    system.log.warn(s"Replying with error in uri (${uri.path.toString()}): [$error]")
    completeWithJson(generateJsonError(error), statusCode)
  }

  private def completeWithOK(): StandardRoute = completeWithJson(Json.Null)

  private def completeWithToken(customerId: CustomerId): Route = setCookie(JwtUtils.getSignedJwtCookie(customerId)) {
    completeWithJson(customerId.asJson)
  }

  private val exceptionHandler: Uri => ExceptionHandler = uri =>
    ExceptionHandler { case throwable =>
      completeWithError(StatusCodes.InternalServerError, throwable.getMessage)(uri)
    }

  private val rejectionHandler: RejectionHandler = RejectionHandler.default.mapRejectionResponse(response =>
    response.transformEntityDataBytes(
      Flow[ByteString]
        .map(rawError => ByteString(generateJsonError(rawError.utf8String).noSpaces))
    )
  )

  private val handleErrors: Uri => Directive[Unit] = uri =>
    handleExceptions(exceptionHandler(uri)).and(handleRejections(rejectionHandler))

  private lazy val UserRoutes: Uri => Route = implicit uri =>
    pathPrefix("user") {
      concat(
        pathPrefix("login") {
          concat(
            pathPrefix("check") {
              pathEndOrSingleSlash {
                get { // GET /user/login/check
                  JwtUtils.withCustomerIdFromToken(uri) { customerId =>
                    completeWithOK()
                  }
                }
              }
            },
            pathEndOrSingleSlash {
              post { // POST /user/login
                entity(as[LoginPayload]) { payload =>
                  checkPayloadIsValid(payload) {
                    loginUser(payload)
                  }
                }
              }
            }
          )
        },
        pathPrefix("logout") {
          pathEndOrSingleSlash {
            post { // POST /user/logout
              deleteCookie(JwtUtils.baseCookie) {
                completeWithOK()
              }
            }
          }
        },
        pathEndOrSingleSlash {
          concat(
            post { // POST /user
              entity(as[UserData]) { userData =>
                checkPayloadIsValid(userData) {
                  askToUsersManagerActor[CustomerId](
                    UsersManager.CreateUser(UUID.randomUUID(), userData)(_),
                    customerId => completeWithToken(customerId)
                  )
                }
              }
            },
            get { // GET /user
              JwtUtils.withCustomerIdFromToken(uri) { customerId =>
                getUser(customerId)
              }
            },
            patch { // PATCH /user
              JwtUtils.withCustomerIdFromToken(uri) { customerId =>
                entity(as[UpdateUserDataPayload]) { payload =>
                  checkPayloadIsValid(payload) {
                    askToUsersManagerActor[Done](
                      UsersManager.UpdateUserData(
                        customerId,
                        payload.maybeEmail,
                        payload.maybeFirstName,
                        payload.maybeLastName,
                        payload.maybeEncryptedPassword
                      )(_),
                      _ => completeWithOK()
                    )
                  }
                }
              }
            }
          )
        }
      )
    }

  private lazy val WalletRoutes: Uri => Route = implicit uri =>
    pathPrefix("wallet") {
      JwtUtils.withCustomerIdFromToken(uri) { customerId =>
        concat(
          pathPrefix("recharge") {
            pathEndOrSingleSlash {
              post { // POST /wallet/recharge
                entity(as[Money]) { amount =>
                  askToTransactionsManagerActor[Done](
                    TransactionsManager.RechargeWallet(UUID.randomUUID(), customerId, amount)(_),
                    _ => completeWithOK()
                  )
                }
              }
            }
          },
          pathPrefix("transfer") {
            concat(
              pathPrefix("scheduler") {
                concat(
                  pathPrefix(JavaUUID) { scheduledOperationId =>
                    pathEndOrSingleSlash {
                      delete { // DELETE /wallet/transfer/schedule/123-456-678
                        askToTransactionsManagerActor[Done](
                          TransactionsManager.DeleteScheduledOperation(customerId, scheduledOperationId)(_),
                          _ => completeWithOK()
                        )
                      }
                    }
                  },
                  pathEndOrSingleSlash {
                    concat(
                      get { // GET /wallet/transfer/schedule
                        getScheduledOperations(customerId)
                      },
                      put { // PUT /wallet/transfer/schedule
                        entity(as[CreateScheduledOperationPayload]) { payload =>
                          checkPayloadIsValid(payload) {
                            createScheduledOperation(customerId, payload)
                          }
                        }
                      }
                    )
                  }
                )
              },
              pathEndOrSingleSlash {
                post { // POST /wallet/transfer
                  entity(as[TransferMoneyPayload]) { payload =>
                    checkPayloadIsValid(payload) {
                      transferMoney(customerId, payload)
                    }
                  }
                }

              }
            )
          },
          pathEndOrSingleSlash {
            get { // GET /wallet
              getWallet(customerId)
            }
          }
        )
      }
    }

  lazy val Routes: Route = extractRequest { request =>
    implicit val uri: Uri = request.uri
    system.log.debug(s"Received ${request.method.value} ${request.uri.path.toString()}")

    cors() {
      handleErrors(request.uri) {
        concat(
          UserRoutes(uri),
          WalletRoutes(uri),
          pathEndOrSingleSlash(complete("Server up and running"))
        )
      }
    }
  }

  private def askToUsersManagerActor[R](command: ActorRef[StatusReply[R]] => UsersManagerCommand, onSuccess: R => Route)(implicit
      system: ActorSystem[Nothing],
      uri: Uri
  ): Route = {
    lazy val future: Future[R] = usersManagerActorRef.askWithStatus[R](replyTo => command(replyTo))

    onComplete(future) {
      case Failure(throwable) =>
        system.log.error(s"Failure in $uri", throwable)
        completeWithError(StatusCodes.InternalServerError, throwable.getMessage)

      case Success(response) =>
        onSuccess(response)
    }
  }

  private def askToTransactionsManagerActor[R](
      command: ActorRef[StatusReply[R]] => TransactionsManagerCommand,
      onSuccess: R => Route
  )(implicit
      system: ActorSystem[Nothing],
      uri: Uri
  ): Route = {
    lazy val future: Future[R] = transactionsManagerActorRef.askWithStatus[R](replyTo => command(replyTo))

    onComplete(future) {
      case Failure(throwable) =>
        system.log.error(s"Failure in $uri", throwable)
        completeWithError(StatusCodes.InternalServerError, throwable.getMessage)

      case Success(response) =>
        onSuccess(response)
    }
  }

  def loginUser(payload: LoginPayload)(implicit system: ActorSystem[Nothing], uri: Uri): Route = {
    lazy val future: Future[CustomerId] = usersManagerActorRef
      .askWithStatus[CustomerId](replyTo => UsersManager.LoginUserWithEmail(payload.email, payload.encryptedPassword)(replyTo))

    onComplete(future) {
      case Failure(throwable) =>
        system.log.error(s"Failure while logging-in user", throwable)
        // Due to security reasons we do not inform about the reason for the failed login (email does not exist etc.)
        completeWithError(StatusCodes.Unauthorized, "Failed to login")

      case Success(customerId) =>
        completeWithToken(customerId)
    }
  }

  def getCustomerId(email: Email): Future[CustomerId] =
    database
      .run(UsersTable.Table.filter(_.email === email).map(_.customerId).result.head)
      .recoverWith { case _: NoSuchElementException =>
        Future.failed(new NoSuchElementException(s"Customer with email ${email.value} not found"))
      }

  def transferMoney(senderCustomerId: CustomerId, payload: TransferMoneyPayload)(implicit
      system: ActorSystem[Nothing],
      uri: Uri
  ): Route = {
    lazy val transferMoney: CustomerId => Future[Done] = recipientCustomerId =>
      transactionsManagerActorRef.askWithStatus[Done](replyTo =>
        TransactionsManager.TransferMoney(
          senderCustomerId,
          recipientCustomerId,
          UUID.randomUUID(),
          payload.description,
          payload.amount
        )(
          replyTo
        )
      )

    onComplete(getCustomerId(payload.recipientEmail).flatMap(recipientCustomerId => transferMoney(recipientCustomerId))) {
      case Failure(throwable) =>
        system.log.error(s"Failure while transferring money", throwable)
        completeWithError(StatusCodes.InternalServerError, throwable.getMessage)

      case Success(_) =>
        completeWithOK()
    }
  }

  def getUser(customerId: CustomerId)(implicit
      system: ActorSystem[Nothing],
      uri: Uri
  ): Route = {
    val getUser: Future[UserRecord] = database.run {
      UsersTable.Table
        .filter(record => record.customerId === customerId)
        .result
        .head
    }

    onComplete(getUser) {
      case Failure(throwable) =>
        system.log.error(s"Failure while getting user", throwable)
        completeWithError(StatusCodes.InternalServerError, throwable.getMessage)

      case Success(userRecord) =>
        completeWithJson(userRecord.asJson)
    }
  }

  def getWallet(customerId: CustomerId)(implicit
      system: ActorSystem[Nothing],
      uri: Uri
  ): Route = {
    val getBalance = database.run {
      UsersBalanceTable.Table
        .filter(record => record.customerId === customerId)
        .map(_.balance)
        .result
        .headOption
        .map(_.getOrElse(Money(0)))
    }

    val getHistory: Future[Seq[(TransactionsHistoryRecord, Option[UserRecord])]] = database.run {
      TransactionsHistoryTable.Table
        .filter(record => record.senderCustomerId === customerId || record.recipientCustomerId === customerId)
        .joinLeft(UsersTable.Table)
        .on { case (transactionRecord, userRecord) =>
          Case
            .If(transactionRecord.senderCustomerId === customerId)
            .Then(transactionRecord.recipientCustomerId === userRecord.customerId)
            .Else(transactionRecord.senderCustomerId === userRecord.customerId)
        }
        .result
    }

    val result: Future[Json] = for {
      balance <- getBalance
      history <- getHistory
    } yield Json.obj("balance" -> balance.asJson, "history" -> history.asJson(encodeSeq(TransactionUserJoinEncoder)))

    onComplete(result) {
      case Failure(throwable) =>
        system.log.error(s"Failure while transferring money", throwable)
        completeWithError(StatusCodes.InternalServerError, throwable.getMessage)

      case Success(json) =>
        completeWithJson(json)
    }
  }

  def getScheduledOperations(customerId: CustomerId)(implicit
      system: ActorSystem[Nothing],
      uri: Uri
  ): Route = {
    val getScheduledOperations: Future[Seq[(ScheduledOperationRecord, UserRecord)]] = database.run {
      ScheduledOperationsTable.Table
        .filter(record => record.senderCustomerId === customerId)
        .join(UsersTable.Table)
        .on { case (scheduledRecord, userRecord) =>
          scheduledRecord.recipientCustomerId === userRecord.customerId
        }
        .result
    }

    onComplete(getScheduledOperations.map(_.asJson(encodeSeq(ScheduledOperationUserJoinEncoder)))) {
      case Failure(throwable) =>
        system.log.error(s"Failure while transferring money", throwable)
        completeWithError(StatusCodes.InternalServerError, throwable.getMessage)

      case Success(json) =>
        completeWithJson(json)
    }
  }

  def createScheduledOperation(customerId: CustomerId, payload: CreateScheduledOperationPayload)(implicit
      system: ActorSystem[Nothing],
      uri: Uri
  ): Route = {
    lazy val createScheduledOperation: CustomerId => Future[Done] = recipientCustomerId =>
      transactionsManagerActorRef.askWithStatus[Done](replyTo =>
        TransactionsManager.CreateScheduledOperation(
          UUID.randomUUID(),
          ScheduledOperation(
            customerId,
            recipientCustomerId,
            payload.amount,
            payload.when,
            payload.description,
            payload.repeat
          )
        )(
          replyTo
        )
      )

    onComplete(
      getCustomerId(payload.recipientEmail).flatMap(recipientCustomerId => createScheduledOperation(recipientCustomerId))
    ) {
      case Failure(throwable) =>
        system.log.error(s"Failure while creating scheduled operation", throwable)
        completeWithError(StatusCodes.InternalServerError, throwable.getMessage)

      case Success(_) =>
        completeWithOK()
    }
  }

  def checkPayloadIsValid[P <: Validable[_]](payload: P)(handleSuccess: Route)(implicit uri: Uri): Route =
    payload.validate() match {
      case Validated.Valid(_) =>
        handleSuccess
      case Validated.Invalid(errors) =>
        completeWithError(StatusCodes.BadRequest, errors.toList.mkString(", "))
    }

}

object EasyPayAppRoutes {

  implicit def circeUnmarshaller[A](implicit decoder: Decoder[A], ec: ExecutionContext): FromEntityUnmarshaller[A] =
    Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map { data =>
      decode[A](data) match {
        case Right(decoded) => decoded
        case Left(error)    => throw IllegalArgumentException(error.getMessage)
      }
    }

  implicit val ScheduledOperationTupleEncoder: Encoder[(ScheduledOperationId, ScheduledOperation)] = Encoder.instance {
    case (scheduledOperationId, scheduledOperation) =>
      scheduledOperation.asJson.deepMerge(Json.obj("id" -> scheduledOperationId.asJson))
  }

}
