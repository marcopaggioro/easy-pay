package it.marcopaggioro.easypay.database

import akka.japi.function.Function
import akka.projection.jdbc.JdbcSession
import it.marcopaggioro.easypay.AppConfig

import java.sql.{Connection, DriverManager}

class PlainJdbcSession extends JdbcSession {

  private lazy val connection = {
    val connection: Connection = DriverManager.getConnection(AppConfig.dbUrl, AppConfig.dbUser, AppConfig.dbPassword)
    connection.setAutoCommit(false)
    connection
  }

  /** withConnection */
  override def withConnection[Result](func: Function[Connection, Result]): Result = func(connection)

  /** commit */
  override def commit(): Unit = connection.commit()

  /** rollback */
  override def rollback(): Unit = connection.rollback()

  /** close */
  override def close(): Unit = connection.close()
}
