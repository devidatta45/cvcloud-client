package com.cvcloud.client.controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import com.cvcloud.client.services._
import com.cvcloud.client.utils.JsonImplicits._
import com.cvcloud.client.utils.{ActorCreator, BaseAuthentication, Constants, FindByIdCommand}
import play.api.mvc.{Action, Controller}
import BaseAuthentication._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
  * Created by Donald Pollock on 01/06/2017.
  */
@Singleton
class CVCUserController @Inject()(creator: ActorCreator) extends Controller{
  implicit val timeout: Timeout = Constants.TIMEOUT
  val userActor = creator.createActorRef(Props(classOf[CVUserActor]), "CVUserActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor]), "SessionActor")

  val login = Action.async { request =>
    val json = request.body.asJson.get
    val loginData = json.toString()
    val login = extractEntity[Login](loginData)
    val loginWithSession = LoginWithSession(login, sessionActor)
    val result = ask(userActor, loginWithSession).mapTo[Future[LoginResult]]
    val actualResult = result.flatMap(identity)
    actualResult.map { act =>
      Ok(toJson(act))
    }
  }

  val createUser = Action.async { request =>
    val json = request.body.asJson.get
    val userData = json.toString()
    val user = extractEntity[CVUser](userData)
    val result = ask(userActor, user).mapTo[Future[Boolean]]
    val actualResult = result.flatMap(identity)
    actualResult.map { act =>
      Ok(toJson(act))
    }
  }
  def findUser(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(userActor, FindByIdCommand(BSONObjectID.parse(id).get)).mapTo[Future[List[CVUser]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          Ok(toJson(res.head))
        }
      }
      else {
        Future(Ok(toJson(StatusMessage("User Session expired"))))
      }
    })
  }
  def logout(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(sessionActor, id).mapTo[Future[Int]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          Ok(toJson(StatusMessage(s"Deleted$res")))
        }
      }
      else {
        Future(Ok(toJson(StatusMessage("User Session expired"))))
      }
    })
  }
}
