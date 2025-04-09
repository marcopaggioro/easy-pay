package it.marcopaggioro.easypay

import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationLong

object AppConfig {

  lazy val config: Config = ConfigFactory.load()

  // Timeouts
  private lazy val timeoutsNode: Config = config.getConfig("timeouts")
  implicit lazy val askTimeout: Timeout = Timeout(timeoutsNode.getDuration("ask").toMillis.millis) // From Java to Scala

  // Database
  private lazy val dbNode: Config = config.getConfig("slick.db")
  lazy val dbUrl: String = dbNode.getString("url")
  lazy val dbHost: String = dbNode.getString("host")
  lazy val dbUser: String = dbNode.getString("user")
  lazy val dbPassword: String = dbNode.getString("password")

  // HTTP
  private lazy val httpNode: Config = config.getConfig("http")
  lazy val httpAddress: String = httpNode.getString("address")
  lazy val httpPort: Int = httpNode.getInt("port")

}
