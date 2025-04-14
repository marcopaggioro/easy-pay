package it.marcopaggioro.easypay

import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import it.marcopaggioro.easypay.domain.classes.Money

import java.time.Period
import scala.concurrent.duration.DurationLong

object AppConfig {

  lazy val config: Config = ConfigFactory.load()

  lazy val startingBalance: Money = Money(10)
  lazy val historyPageSize: Int = 10
  lazy val interactedUsersSize: Int = 10
  lazy val minScheduledOperationPeriod: Period = Period.ofDays(1)

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
