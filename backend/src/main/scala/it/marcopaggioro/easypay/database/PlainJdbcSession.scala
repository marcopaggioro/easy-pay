package it.marcopaggioro.easypay.database

import akka.japi.function.Function
import akka.projection.jdbc.JdbcSession
import it.marcopaggioro.easypay.AppConfig

import java.sql.{Connection, DriverManager}

//TODO No DRI found for query
class PlainJdbcSession extends JdbcSession {

  private lazy val connection = {
    val connection: Connection = DriverManager.getConnection(AppConfig.dbUrl, AppConfig.dbUser, AppConfig.dbPassword)
    connection.setAutoCommit(false)
    connection
  }

  override def withConnection[Result](func: Function[Connection, Result]): Result = func(connection)

  override def commit(): Unit = connection.commit()

  override def rollback(): Unit = connection.rollback()

  override def close(): Unit = connection.close()
}