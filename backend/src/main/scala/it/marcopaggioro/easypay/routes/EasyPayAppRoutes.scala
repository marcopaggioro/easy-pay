package it.marcopaggioro.easypay.routes

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler, SupervisorStrategy}
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.*
import akka.http.scaladsl.server.directives.BasicDirectives.extractRequest
import akka.http.scaladsl.settings.CorsSettings
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.pattern.StatusReply
import akka.pattern.StatusReply.ErrorMessage
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.util.ByteString
import akka.{Done, NotUsed}
import cats.data.Validated
import io.circe.Encoder.encodeSeq
import io.circe.*
import io.circe.jawn.decode
import io.circe.syntax.EncoderOps
import it.marcopaggioro.easypay.AppConfig
import it.marcopaggioro.easypay.AppConfig.askTimeout
import it.marcopaggioro.easypay.EasyPayApp.{completeWithError, completeWithJson, generateJsonError}
import it.marcopaggioro.easypay.actor.WebSocketsManagerActor.WebSocketsManagerActorCommand
import it.marcopaggioro.easypay.actor.{TransactionsManagerActor, UsersManagerActor, WebSocketsManagerActor}
import it.marcopaggioro.easypay.database.PostgresProfile.*
import it.marcopaggioro.easypay.database.PostgresProfile.api.*
import it.marcopaggioro.easypay.database.PostgresProfile.InstantMapper
import it.marcopaggioro.easypay.database.scheduledoperations.ScheduledOperationRecord.ScheduledOperationUserJoinEncoder
import it.marcopaggioro.easypay.database.scheduledoperations.{ScheduledOperationRecord, ScheduledOperationsTable}
import it.marcopaggioro.easypay.database.transactionshistory.TransactionsHistoryRecord.TransactionUserJoinEncoder
import it.marcopaggioro.easypay.database.transactionshistory.{TransactionsHistoryRecord, TransactionsHistoryTable}
import it.marcopaggioro.easypay.database.users.UserRecord.{UserRecordEncoder, UserRecordInteractedEncoder}
import it.marcopaggioro.easypay.database.users.{UserRecord, UsersTable}
import it.marcopaggioro.easypay.database.usersbalance.UsersBalanceTable
import it.marcopaggioro.easypay.domain.TransactionsManager.TransactionsManagerCommand
import it.marcopaggioro.easypay.domain.UsersManager.UsersManagerCommand
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, ScheduledOperationId}
import it.marcopaggioro.easypay.domain.classes.userdata.{Email, UserData}
import it.marcopaggioro.easypay.domain.classes.{Money, ScheduledOperation, Validable}
import it.marcopaggioro.easypay.domain.{TransactionsManager, UsersManager}
import it.marcopaggioro.easypay.routes.EasyPayAppRoutes.circeUnmarshaller
import it.marcopaggioro.easypay.routes.payloads.LoginPayload.LoginPayloadDecoder
import it.marcopaggioro.easypay.routes.payloads.{
  CreateScheduledOperationPayload,
  GetOperationsPayload,
  LoginPayload,
  TransferMoneyPayload,
  UpdateUserDataPayload
}
import it.marcopaggioro.easypay.utilities.{JwtUtils, ValidationUtilities}
import slick.jdbc.JdbcBackend.Database

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

class EasyPayAppRoutes(webSocketManagerActorRef: ActorRef[WebSocketsManagerActorCommand], database: Database)(implicit
    system: ActorSystem[Nothing]
) {

  private implicit val scheduler: Scheduler = system.scheduler
  private implicit val executionContext: ExecutionContextExecutor = system.executionContext

  private val usersManagerActorRef: ActorRef[UsersManagerCommand] = system.systemActorOf(
    Behaviors.supervise(UsersManagerActor()).onFailure[Exception](SupervisorStrategy.restart),
    UsersManagerActor.Name
  )
  private val transactionsManagerActorRef: ActorRef[TransactionsManagerCommand] = system.systemActorOf(
    Behaviors.supervise(TransactionsManagerActor()).onFailure[Exception](SupervisorStrategy.restart),
    TransactionsManagerActor.Name
  )

  private def completeWithOK(): StandardRoute = completeWithJson(Json.obj("ok" -> true.asJson))

  private def completeWithToken(customerId: CustomerId): Route = {
    val httpCookie: HttpCookie = JwtUtils.getSignedJwtCookie(customerId)
    setCookie(httpCookie) {
      completeWithJson(Json.obj("customerId" -> customerId.asJson, "expiration" -> httpCookie.expires.map(_.clicks).asJson))
    }
  }

  private val exceptionHandler: Uri => ExceptionHandler = implicit uri =>
    ExceptionHandler { case throwable =>
      completeWithError(StatusCodes.InternalServerError, throwable.getMessage)
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
            path("check") {
              get { // GET /user/login/check
                JwtUtils.withCustomerIdFromToken() { customerId =>
                  completeWithOK()
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
        path("logout") {
          post { // POST /user/logout
            deleteCookie(JwtUtils.baseCookie) {
              completeWithOK()
            }
          }
        },
        pathEndOrSingleSlash {
          concat(
            post { // POST /user
              entity(as[UserData]) { userData =>
                checkPayloadIsValid(userData) {
                  createUser(userData)
                }
              }
            },
            get { // GET /user
              JwtUtils.withCustomerIdFromToken() { customerId =>
                getUser(customerId)
              }
            },
            patch { // PATCH /user
              JwtUtils.withCustomerIdFromToken() { customerId =>
                entity(as[UpdateUserDataPayload]) { payload =>
                  checkPayloadIsValid(payload) {
                    askToActor[UsersManagerCommand, Done](
                      usersManagerActorRef,
                      UsersManager.UpdateUserData(
                        customerId,
                        payload.maybeFirstName,
                        payload.maybeLastName,
                        payload.maybeBirthDate,
                        payload.maybeEmail,
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
    JwtUtils.withCustomerIdFromToken() { customerId =>
      pathPrefix("wallet") {
        concat(
          path("recharge") {
            post { // POST /wallet/recharge
              entity(as[Money]) { amount =>
                askToActor[TransactionsManagerCommand, Done](
                  transactionsManagerActorRef,
                  TransactionsManager.RechargeWallet(UUID.randomUUID(), customerId, amount)(_),
                  _ => completeWithOK()
                )
              }
            }
          },
          pathPrefix("transfer") {
            concat(
              pathPrefix("scheduler") {
                concat(
                  path(JavaUUID) { scheduledOperationId =>
                    delete { // DELETE /wallet/transfer/schedule/{scheduledOperationId}
                      askToActor[TransactionsManagerCommand, Done](
                        transactionsManagerActorRef,
                        TransactionsManager.DeleteScheduledOperation(customerId, scheduledOperationId)(_),
                        _ => completeWithOK()
                      )
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
          path("interacted-customers") {
            get { // GET /wallet/interacted-customers
              getInteractedCustomers(customerId)
            }
          },
          path("balance") {
            get { // GET /wallet/balance
              getBalance(customerId)
            }
          },
          path("operations") {
            post { // POST /wallet/operations
              entity(as[GetOperationsPayload]) { payload =>
                checkPayloadIsValid(payload) {
                  getOperations(customerId, payload)
                }
              }
            }
          }
        )
      }
    }

  private def webSocketFlow(customerId: CustomerId): Flow[Message, Message, NotUsed] = {
    val (clientActor, outSource): (ActorRef[TextMessage.Strict], Source[Message, NotUsed]) = ActorSource
      .actorRef[TextMessage.Strict](
        PartialFunction.empty,
        PartialFunction.empty,
        100,
        OverflowStrategy.fail
      )
      .preMaterialize()

    webSocketManagerActorRef.tell(WebSocketsManagerActor.Register(customerId, clientActor))

    Flow.fromSinkAndSourceCoupled(Sink.head[Message], outSource).watchTermination() { case (notUsed, terminationFuture) =>
      terminationFuture.onComplete(_ => webSocketManagerActorRef ! WebSocketsManagerActor.Unregister(customerId, clientActor))
      notUsed
    }
  }

  private val WebSocketRoutes: Uri => Route = implicit uri =>
    JwtUtils.withCustomerIdFromToken() { customerId =>
      path("ws") {
        handleWebSocketMessages(webSocketFlow(customerId))
      }
    }

  lazy val Routes: Route = extractRequest { request =>
    implicit val uri: Uri = request.uri
    system.log.debug(s"Received ${request.method.value} ${request.uri.path.toString()}")

    concat(
      cors(CorsSettings(system).withAllowAnyOrigin()) {
        path("swagger") {
          getFromResource("swagger.yaml", ContentTypes.`text/plain(UTF-8)`)
        }
      },
      cors() {
        handleErrors(request.uri) {
          concat(
            UserRoutes(uri),
            WalletRoutes(uri),
            WebSocketRoutes(uri),
            pathEndOrSingleSlash(complete("Server up and running"))
          )
        }
      }
    )
  }

  private def askToActor[C, R](actorRef: ActorRef[C], command: ActorRef[StatusReply[R]] => C, onSuccess: R => Route)(implicit
      system: ActorSystem[Nothing],
      uri: Uri
  ): Route =
    onComplete(actorRef.askWithStatus[R](replyTo => command(replyTo))) {
      case Failure(throwable) =>
        system.log.error(s"Failure in $uri", throwable)
        throwable match {
          case errorMessage: ErrorMessage =>
            completeWithError(StatusCodes.InternalServerError, errorMessage.getMessage)

          case _ =>
            completeWithError(StatusCodes.InternalServerError, ValidationUtilities.GenericError)
        }

      case Success(response) =>
        onSuccess(response)
    }

  private def createUser(userData: UserData)(implicit system: ActorSystem[Nothing], uri: Uri): Route = {
    val result: Future[CustomerId] = for {
      customerId <- usersManagerActorRef
        .askWithStatus[CustomerId](replyTo => UsersManager.CreateUser(UUID.randomUUID(), userData)(replyTo))
      _ <- transactionsManagerActorRef
        .askWithStatus[Done](replyTo =>
          TransactionsManager.RechargeWallet(UUID.randomUUID(), customerId, AppConfig.startingBalance)(replyTo)
        )
    } yield customerId

    onComplete(result) {
      case Failure(throwable) =>
        system.log.error(s"Failure while creating user", throwable)
        throwable match {
          case errorMessage: ErrorMessage =>
            completeWithError(StatusCodes.InternalServerError, errorMessage.getMessage)

          case _ =>
            completeWithError(StatusCodes.InternalServerError, ValidationUtilities.GenericError)
        }

      case Success(customerId) =>
        completeWithToken(customerId)
    }
  }

  private def loginUser(payload: LoginPayload)(implicit system: ActorSystem[Nothing], uri: Uri): Route = {
    lazy val future: Future[CustomerId] = usersManagerActorRef
      .askWithStatus[CustomerId](replyTo => UsersManager.LoginUserWithEmail(payload.email, payload.encryptedPassword)(replyTo))

    onComplete(future) {
      case Failure(throwable) =>
        system.log.error(s"Failure while logging-in user", throwable)
        // Due to security reasons we do not inform about the reason for the failed login (email does not exist etc.)
        completeWithError(StatusCodes.Unauthorized, "Credenziali invalide")

      case Success(customerId) =>
        completeWithToken(customerId)
    }
  }

  private def getCustomerId(email: Email): Future[CustomerId] =
    database
      .run(UsersTable.Table.filter(_.email === email).map(_.customerId).result.head)
      .recoverWith { case _: NoSuchElementException =>
        Future.failed(new NoSuchElementException(s"Email ${email.value} non trovata"))
      }

  private def transferMoney(senderCustomerId: CustomerId, payload: TransferMoneyPayload)(implicit
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
        system.log.error(s"Failure while transferring money $uri", throwable)
        throwable match {
          case errorMessage: ErrorMessage =>
            completeWithError(StatusCodes.InternalServerError, errorMessage.getMessage)

          case _: NoSuchElementException =>
            completeWithError(StatusCodes.NotFound, throwable.getMessage)

          case _ =>
            completeWithError(StatusCodes.InternalServerError, ValidationUtilities.GenericError)
        }

      case Success(_) =>
        completeWithOK()
    }
  }

  private def getUser(customerId: CustomerId)(implicit system: ActorSystem[Nothing], uri: Uri): Route = {
    val getUser: Future[Option[UserRecord]] = database
      .run {
        UsersTable.Table
          .filter(record => record.customerId === customerId)
          .result
          .headOption
      }

    onComplete(getUser) {
      case Failure(throwable) =>
        system.log.error(s"Failure while getting user", throwable)
        completeWithError(StatusCodes.InternalServerError, ValidationUtilities.GenericError)

      case Success(None) =>
        completeWithError(StatusCodes.NotFound, s"Cliente $customerId non trovato")

      case Success(Some(userRecord)) =>
        completeWithJson(userRecord.asJson)
    }
  }

  private def getInteractedCustomers(customerId: CustomerId)(implicit
      system: ActorSystem[Nothing],
      uri: Uri
  ): Route = {
    val getInteractedUsers: Future[Seq[UserRecord]] = database.run {
      TransactionsHistoryTable.Table
        .filter(record =>
          record.senderCustomerId =!= record.recipientCustomerId && (record.senderCustomerId === customerId || record.recipientCustomerId === customerId)
        )
        .join(UsersTable.Table)
        .on { case (transactionRecord, userRecord) =>
          Case
            .If(transactionRecord.senderCustomerId === customerId)
            .Then(transactionRecord.recipientCustomerId === userRecord.customerId)
            .Else(transactionRecord.senderCustomerId === userRecord.customerId)
        }
        .map(_._2)
        .distinctOn(_.customerId)
        .take(AppConfig.interactedUsersSize)
        .result
    }

    onComplete(getInteractedUsers) {
      case Failure(throwable) =>
        system.log.error(s"Failure while getting interacted customers", throwable)
        completeWithError(StatusCodes.InternalServerError, ValidationUtilities.GenericError)

      case Success(interactedUsers) =>
        completeWithJson(interactedUsers.asJson(encodeSeq(UserRecordInteractedEncoder)))
    }
  }

  private def getBalance(customerId: CustomerId)(implicit system: ActorSystem[Nothing], uri: Uri): Route = {
    val getBalance: Future[Money] = database.run {
      UsersBalanceTable.Table
        .filter(record => record.customerId === customerId)
        .map(_.balance)
        .result
        .headOption
        .map(_.getOrElse(Money(0)))
    }

    onComplete(getBalance) {
      case Failure(throwable) =>
        system.log.error(s"Failure while getting balance", throwable)
        completeWithError(StatusCodes.InternalServerError, ValidationUtilities.GenericError)

      case Success(balance) =>
        completeWithJson(balance.asJson)
    }
  }

  private def getOperations(customerId: CustomerId, payload: GetOperationsPayload)(implicit
      system: ActorSystem[Nothing],
      uri: Uri
  ): Route = {
    val baseHistoryQuery = TransactionsHistoryTable.Table
      .filter(record => record.senderCustomerId === customerId || record.recipientCustomerId === customerId)
      .filterOpt(payload.maybeStartDate) { case (record, startDate) =>
        record.instant >= LiteralColumn(startDate)(InstantMapper)
      }
      .filterOpt(payload.maybeEndDate) { case (record, endDate) =>
        record.instant <= LiteralColumn(endDate)(InstantMapper)
      }
      .join(UsersTable.Table)
      .on { case (transactionRecord, userRecord) =>
        Case
          .If(transactionRecord.senderCustomerId === customerId)
          .Then(transactionRecord.recipientCustomerId === userRecord.customerId)
          .Else(transactionRecord.senderCustomerId === userRecord.customerId)
      }

    val getHistoryCount: Future[Int] = database.run {
      baseHistoryQuery.result.map(_.size)
    }

    val offset: Int = (payload.page - 1) * AppConfig.historyPageSize
    val getHistory: Future[Seq[(TransactionsHistoryRecord, UserRecord)]] = database.run {
      baseHistoryQuery
        .sortBy(_._1.instant.desc)
        .drop(offset)
        .take(AppConfig.historyPageSize)
        .result
    }

    val result: Future[Json] = for {
      historyCount <- getHistoryCount
      history <- getHistory
    } yield Json.obj(
      "pageSize" -> AppConfig.historyPageSize.asJson,
      "historyCount" -> historyCount.asJson,
      "history" -> history.asJson(encodeSeq(TransactionUserJoinEncoder))
    )

    onComplete(result) {
      case Failure(throwable) =>
        system.log.error(s"Failure while getting operations", throwable)
        completeWithError(StatusCodes.InternalServerError, ValidationUtilities.GenericError)

      case Success(json) =>
        completeWithJson(json)
    }
  }

  private def getScheduledOperations(customerId: CustomerId)(implicit system: ActorSystem[Nothing], uri: Uri): Route = {
    val getScheduledOperations: Future[Seq[(ScheduledOperationRecord, UserRecord)]] = database.run {
      ScheduledOperationsTable.Table
        .filter(record => record.senderCustomerId === customerId)
        .join(UsersTable.Table)
        .on { case (scheduledRecord, userRecord) =>
          scheduledRecord.recipientCustomerId === userRecord.customerId
        }
        .sortBy(_._1.when)
        .result
    }

    onComplete(getScheduledOperations.map(_.asJson(encodeSeq(ScheduledOperationUserJoinEncoder)))) {
      case Failure(throwable) =>
        system.log.error(s"Failure while getting scheduled operations", throwable)
        completeWithError(StatusCodes.InternalServerError, ValidationUtilities.GenericError)

      case Success(json) =>
        completeWithJson(json)
    }
  }

  private def createScheduledOperation(customerId: CustomerId, payload: CreateScheduledOperationPayload)(implicit
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
        throwable match {
          case errorMessage: ErrorMessage =>
            completeWithError(StatusCodes.InternalServerError, errorMessage.getMessage)

          case _: NoSuchElementException =>
            completeWithError(StatusCodes.NotFound, throwable.getMessage)

          case _ =>
            completeWithError(StatusCodes.InternalServerError, ValidationUtilities.GenericError)
        }

      case Success(_) =>
        completeWithOK()
    }
  }

  private def checkPayloadIsValid[P <: Validable[_]](
      payload: P
  )(handleSuccess: Route)(implicit system: ActorSystem[Nothing], uri: Uri): Route =
    payload.validate() match {
      case Validated.Valid(_) =>
        handleSuccess

      case Validated.Invalid(errors) =>
        val errorsFlattened: String = errors.toList.mkString(", ")
        system.log.warn(s"Invalid payload in $uri: $errorsFlattened")
        completeWithError(StatusCodes.BadRequest, errorsFlattened)
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

  val CustomerIdSegment: PathMatcher1[CustomerId] = Segment.flatMap { segment =>
    Try(UUID.fromString(segment)).toOption
  }

}
