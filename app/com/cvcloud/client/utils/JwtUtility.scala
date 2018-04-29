package com.cvcloud.client.utils

/**
  * Created by Donald Pollock on 03/07/2017.
  */
import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}

class JwtUtility {
  val JwtSecretKey = "secretKey"
  val JwtSecretAlgo = "HS256"

  def createToken(payload: String): String = {
    val header = JwtHeader(JwtSecretAlgo)
    val claimsSet = JwtClaimsSet(payload)

    JsonWebToken(header, claimsSet, JwtSecretKey)
  }

  def isValidToken(jwtToken: String): Boolean =
    JsonWebToken.validate(jwtToken, JwtSecretKey)

  def decodePayload(jwtToken: String): Option[String] =
    jwtToken match {
      case JsonWebToken(header, claimsSet, signature) => Option(claimsSet.asJsonString)
      case _ => None
    }
}

object JwtUtility extends JwtUtility