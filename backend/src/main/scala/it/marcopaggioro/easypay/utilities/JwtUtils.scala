package it.marcopaggioro.easypay.utilities

import akka.actor.typed.ActorSystem
import akka.http.javadsl.model.headers.SameSite
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.{DateTime, StatusCodes, Uri}
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{optionalCookie, provide}
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.jawn.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import it.marcopaggioro.easypay.EasyPayApp.completeWithError
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.exceptions.JwtException
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim, JwtOptions}

import java.time.{Duration, Instant}
import scala.util.{Failure, Success, Try}

object JwtUtils extends LazyLogging {

  private lazy val TokenAlgorithm: JwtHmacAlgorithm = JwtAlgorithm.HS512
  private lazy val TokenEncryptionSecret = "super-secret-key"
  private lazy val TokenDuration: Duration = Duration.ofMinutes(5)
  private lazy val RefreshTokenDuration: Duration = Duration.ofHours(8)
  private lazy val TokenCookieName: String = "EasyPayToken"
  lazy val RefreshTokenCookieName: String = "EasyPayRefreshToken"
  private lazy val TokenCustomerField: String = "customerId"

  case class JwtContent(customerId: CustomerId)
  implicit val JwtContentDecoder: Decoder[JwtContent] = deriveDecoder[JwtContent]
  implicit val JwtContentEncoder: Encoder[JwtContent] = deriveEncoder[JwtContent]

  private def generateSignedClaim(customerId: CustomerId, duration: Duration): String = {
    val jwtClaim: JwtClaim = JwtClaim(
      expiration = Some(Instant.now.plus(duration).getEpochSecond),
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
  ).withSameSite(SameSite.Strict)

  lazy val baseRefreshCookie: HttpCookie = HttpCookie(
    name = RefreshTokenCookieName,
    value = "",
    secure = true,
    httpOnly = true,
    path = Some("/user/refresh-token")
  ).withSameSite(SameSite.Strict)

  def getSignedJwtCookie(customerId: CustomerId): HttpCookie =
    baseCookie.withValue(generateSignedClaim(customerId, TokenDuration))

  def getSignedRefreshJwtCookie(customerId: CustomerId): HttpCookie =
    baseRefreshCookie.withValue(generateSignedClaim(customerId, RefreshTokenDuration))

  private def decodeToken(token: String): Try[JwtClaim] = JwtCirce.decode(
    token,
    TokenEncryptionSecret,
    Seq(TokenAlgorithm),
    JwtOptions(signature = true, expiration = true, notBefore = true)
  )

  private def withAuthCookie(cookieName: String)(implicit uri: Uri, system: ActorSystem[Nothing]): Directive1[HttpCookie] =
    optionalCookie(cookieName).flatMap {
      case Some(cookie) =>
        provide(cookie.toCookie)

      case None =>
        logger.warn(s"Received request without token in ${uri.path.toString()}")
        completeWithError(StatusCodes.Unauthorized, "Token non trovato")
    }

  def withCustomerIdFromToken(
      cookieName: String = TokenCookieName
  )(implicit uri: Uri, system: ActorSystem[Nothing]): Directive1[CustomerId] = withAuthCookie(cookieName).flatMap { cookie =>
    decodeToken(cookie.value).flatMap(jwtClaim => decode[JwtContent](jwtClaim.content).toTry) match {
      case Failure(exception: JwtException) =>
        logger.warn(s"Invalid or expired token in ${uri.path.toString()}: [${exception.getMessage}]")
        completeWithError(StatusCodes.Unauthorized, "Token invalido o scaduto")

      case Failure(exception) =>
        logger.warn(s"Error while getting customer from token", exception)
        completeWithError(StatusCodes.Unauthorized, "Token invalido o scaduto")

      case Success(jwtContent) =>
        provide(jwtContent.customerId)
    }
  }

}
