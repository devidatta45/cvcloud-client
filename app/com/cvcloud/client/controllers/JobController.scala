package com.cvcloud.client.controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.util.Timeout
import com.cvcloud.client.services._
import com.cvcloud.client.utils.BaseAuthentication._
import com.cvcloud.client.utils.JsonImplicits._
import com.cvcloud.client.utils.{ActorCreator, Constants, FindByIdCommand}
import play.api.mvc.{Action, Controller}
import akka.pattern.ask
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Donald Pollock on 01/06/2017.
  */
@Singleton
class JobController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val jobActor = creator.createActorRef(Props(classOf[JobActor]), "JobActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor]), "SessionActor2")

  val createJob = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val clientData = json.toString()
        val jobCommand = extractEntity[JobCommand](clientData)
        val result = ask(jobActor, jobCommand).mapTo[Future[List[CVCClient]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { act =>
          if (act.nonEmpty) Ok(toJson(act))
          else Ok(toJson(Nil))
        }
      }
      else {
        Future(Ok(toJson(StatusMessage("User Session expired"))))
      }
    })
  }

  def findJob(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(jobActor, JobReferenceCommand(id)).mapTo[Future[List[Job]]]
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

  def updateJobStatus(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(jobActor, FindByIdCommand(BSONObjectID.parse(id).get)).mapTo[Future[List[Job]]]
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

  val updateJob = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val clientData = json.toString()
        val updateCommand = extractEntity[SkillUpdateCommand](clientData)
        val result = ask(jobActor, updateCommand).mapTo[Future[List[Job]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { act =>
          if (act.nonEmpty) Ok(toJson(act))
          else Ok(toJson(Nil))
        }
      }
      else {
        Future(Ok(toJson(StatusMessage("User Session expired"))))
      }
    })
  }
}
