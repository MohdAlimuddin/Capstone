package org.knoldus.JWT


import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}
import spray.json._
import java.util.concurrent.TimeUnit
import scala.util._


object JwtAuthorization extends UserJson {

  def createToken(role: String, expirationPeriodInMinutes: Int): String = {
    val claims = JwtClaim(
      content = s"""{"role": "$role"}""".parseJson.asJsObject.toString,
      expiration = Some(System.currentTimeMillis() / 1000 + TimeUnit.MINUTES.toSeconds(expirationPeriodInMinutes)),
      issuedAt = Some(System.currentTimeMillis() / 1000),
      issuer = Some("knoldus.com")
    )
    JwtSprayJson.encode(claims,"JWT_AUTH", JwtAlgorithm.HS256)
  }

  def tokenDecoder(token : String): Try[JwtClaim] = JwtSprayJson.decode(token,"JWT_AUTH", Seq(JwtAlgorithm.HS256))

  def isTokenValidOrNot(token: String): Boolean = {
    JwtSprayJson.isValid(token,"JWT_AUTH", Seq(JwtAlgorithm.HS256))
  }
}
