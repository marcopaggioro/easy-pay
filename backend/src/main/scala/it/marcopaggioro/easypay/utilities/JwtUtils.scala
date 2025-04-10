package it.marcopaggioro.easypay.utilities

import akka.http.javadsl.model.headers.SameSite
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.{DateTime, HttpEntity, StatusCodes, Uri}
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{complete, optionalCookie, provide}
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.jawn.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim, JwtOptions}

import java.time.{Duration, Instant}
import scala.util.{Failure, Success, Try}

object JwtUtils extends LazyLogging {

  private lazy val TokenAlgorithm: JwtHmacAlgorithm = JwtAlgorithm.HS512
  private lazy val TokenEncryptionSecret = "super-secret-key"
  private lazy val TokenDuration: Duration = Duration.ofHours(1)
  lazy val TokenCookieName: String = "EasyPayToken"
  private lazy val TokenCustomerField: String = "customerId"

  case class JwtContent(customerId: CustomerId)
  implicit val JwtContentDecoder: Decoder[JwtContent] = deriveDecoder[JwtContent]
  implicit val JwtContentEncoder: Encoder[JwtContent] = deriveEncoder[JwtContent]

  private def generateSignedClaim(customerId: CustomerId, expiration: DateTime): String = {
    val jwtClaim: JwtClaim = JwtClaim(
      expiration = Some(Instant.now.plus(TokenDuration).getEpochSecond),
      issuedAt = Some(Instant.now.getEpochSecond),
      content = JwtContent(customerId).asJson.noSpaces
    )
    JwtCirce.encode(jwtClaim, TokenEncryptionSecret, TokenAlgorithm)
  }

  lazy val baseCookie: HttpCookie = HttpCookie(
    name = TokenCookieName,
    value = "",
    secure = true,
    httpOnly = true,
    path = Some("/")
  ).withSameSite(SameSite.None)

  def getSignedJwtCookie(customerId: CustomerId): HttpCookie = {
    val expirationDate = DateTime.now.plus(TokenDuration.toMillis)
    baseCookie
      .withValue(generateSignedClaim(customerId, expirationDate))
      .withExpires(expirationDate)
  }

  private def decodeToken(token: String): Try[JwtClaim] = JwtCirce.decode(
    token,
    TokenEncryptionSecret,
    Seq(TokenAlgorithm),
    JwtOptions(signature = true, expiration = true, notBefore = true)
  )

  private def withAuthCookie(implicit uri: Uri): Directive1[HttpCookie] = optionalCookie(TokenCookieName).flatMap {
    case Some(cookie) =>
      provide(cookie.toCookie)

    case None =>
      logger.warn(s"Received request without token in ${uri.path.toString()}")
      complete(StatusCodes.Unauthorized)
  }

  def withCustomerIdFromToken(implicit uri: Uri): Directive1[CustomerId] = withAuthCookie.flatMap { cookie =>
    decodeToken(cookie.value).flatMap(jwtClaim => decode[JwtContent](jwtClaim.content).toTry) match {
      case Failure(exception) =>
        logger.warn(s"Invalid or expired token in ${uri.path.toString()}", exception)
        complete(StatusCodes.Unauthorized)

      case Success(jwtContent) =>
        provide(jwtContent.customerId)
    }
  }

}
