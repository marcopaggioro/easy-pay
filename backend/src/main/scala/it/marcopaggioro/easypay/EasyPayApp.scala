package it.marcopaggioro.easypay

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.serialization.jackson.CborSerializable
import buildinfo.BuildInfo
import it.marcopaggioro.easypay.actor.UsersManagerActor
import it.marcopaggioro.easypay.actor.projection.{TransactionsProjectorActor, UsersManagerProjectorActor}
import it.marcopaggioro.easypay.domain.UsersManager.UsersManagerCommand
import it.marcopaggioro.easypay.routes.EasyPayAppRoutes
import org.flywaydb.core.Flyway
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Future
import scala.util.{Failure, Success}

object EasyPayApp {

  sealed trait AppCommand extends CborSerializable
  private final case class ServerStarted(serverBinding: ServerBinding) extends AppCommand
  private final case class ServerStartupFailed(cause: Throwable) extends AppCommand

  private def startProjectors(database: Database)(implicit system: ActorSystem[Nothing]): Unit = {
    UsersManagerProjectorActor.startProjectorActor(database, system)
    TransactionsProjectorActor.startProjectorActor(database, system)
  }

  private def started(serverBinding: ServerBinding): Behavior[AppCommand] = Behaviors.setup[AppCommand] { context =>
    context.log.info(s"Server now online at ${serverBinding.localAddress}")
    context.log.info(s"Application version: ${BuildInfo.version}")
    context.log.info(s"Actor system name: ${context.system.name}")

    Behaviors.receiveMessage { unexpected =>
      context.log.warn(s"[started] Received unexpected command ${unexpected.getClass.getSimpleName}")
      Behaviors.same
    }
  }

  private def starting(): Behavior[AppCommand] = Behaviors.setup[AppCommand] { context =>
    Behaviors.receiveMessage {
      case ServerStarted(serverBinding) =>
        context.log.info("[starting] Server startup succeed")
        started(serverBinding)
      case ServerStartupFailed(cause) =>
        context.log.error("[starting] Server startup failed", cause)
        throw new RuntimeException(cause)
    }
  }

  def apply(): Behavior[AppCommand] = Behaviors.setup[AppCommand] { context =>
    implicit val system: ActorSystem[Nothing] = context.system

    val database: Database = Database.forConfig("slick.db", AppConfig.config)
    val usersManagerActorRef: ActorRef[UsersManagerCommand] = system.systemActorOf(
      Behaviors.supervise(UsersManagerActor()).onFailure[Exception](SupervisorStrategy.restart),
      UsersManagerActor.Name
    )
    startProjectors(database)

    val webServerBinding: Future[ServerBinding] = Http()
      .newServerAt(AppConfig.httpAddress, AppConfig.httpPort)
      .bind(new EasyPayAppRoutes(usersManagerActorRef, database).Routes)

    context.pipeToSelf(webServerBinding) {
      case Failure(exception)     => ServerStartupFailed(exception)
      case Success(serverBinding) => ServerStarted(serverBinding)
    }

    starting()
  }

  // TODO commenti ovunque
  // TODO numero di telefono su utente? login etc
  def main(args: Array[String]): Unit = {
    Flyway.configure().dataSource(AppConfig.dbUrl, AppConfig.dbUser, AppConfig.dbPassword).load().migrate()

    ActorSystem(EasyPayApp(), BuildInfo.name)
  }

}
