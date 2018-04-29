package com.cvcloud.client.controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.util.Timeout
import com.cvcloud.client.services.{CVCSkill, CVCSkillActor, SessionActor}
import com.cvcloud.client.utils.BaseAuthentication._
import com.cvcloud.client.utils.JsonImplicits._
import com.cvcloud.client.utils.{ActorCreator, Constants, FindAllCommand}
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask

import scala.concurrent.Future

/**
  * Created by Donald Pollock on 20/06/2017.
  */
@Singleton
class CVCSkillController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val skillActor = creator.createActorRef(Props(classOf[CVCSkillActor]), "CVCSkillActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor]), "SessionActor21")

  val createSkill = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val clientData = json.toString()
        val cvcSkill = extractEntity[CVCSkill](clientData)
        val result = ask(skillActor, cvcSkill).mapTo[Future[Boolean]]
        val actualResult = result.flatMap(identity)
        actualResult.map { act =>
          Ok(toJson(act))
        }
      }
      else {
        Future(Ok(toJson(StatusMessage("User Session expired"))))
      }
    })
  }

  val getAllSkills = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(skillActor, FindAllCommand).mapTo[Future[List[CVCSkill]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          Ok(toJson(res))
        }
      }
      else {
        Future(Ok(toJson(StatusMessage("User Session expired"))))
      }
    })
  }
}
