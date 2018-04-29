package com.cvcloud.client.utils

import akka.actor.ActorRef
import akka.pattern.ask
import com.cvcloud.client.services.{AuthenticationCommand, Session}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import JsonImplicits._

/**
  * Created by Donald Pollock on 19/05/2017.
  */
trait BaseAuthentication {
  def auth(actorRef: ActorRef, apiKey: Option[String]): Future[List[Session]]
}

object BaseAuthentication extends BaseAuthentication {
  def auth(actorRef: ActorRef, apiKey: Option[String]): Future[List[Session]] = if (apiKey.isDefined) {
    if (apiKey.get.equals(TestApiKey.API_KEY)) {
      val session = Session("", "", "")
      Future(List(session))
    }
    else {
      val decodedApiKey = if (JwtUtility.isValidToken(apiKey.get)) {
        JwtUtility.decodePayload(apiKey.get)
      } else None
      val key = if (decodedApiKey.isDefined) Some(extractEntity[Token](decodedApiKey.get)) else None
      if (key.isDefined && key.get.key.equals(TestApiKey.API_KEY)) {
        val session = Session("", "", "")
        Future(List(session))
      } else if (key.isDefined && !key.get.key.equals(TestApiKey.API_KEY)) {
        val session = ask(actorRef, AuthenticationCommand(key.get.key))(Constants.TIMEOUT).mapTo[Future[List[Session]]]
        val existingSession = session.flatMap(identity)
        existingSession
      }
      else {
        val future = Future(Nil)
        future
      }
    }
  }
  else {
    val future = Future(Nil)
    future
  }
}

object TestApiKey {
  val API_KEY = "8190b575-f3ee-41b2-bede-7ddc9e156df2"
}