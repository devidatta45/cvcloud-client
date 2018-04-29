package com.cvcloud.client.controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.util.Timeout
import com.cvcloud.client.services._
import com.cvcloud.client.utils.BaseAuthentication._
import com.cvcloud.client.utils.JsonImplicits._
import com.cvcloud.client.utils._
import play.api.mvc.{Action, Controller}
import akka.pattern.ask
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Donald Pollock on 09/06/2017.
  */
@Singleton
class JobApplicationController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val jobApplicationActor = creator.createActorRef(Props(classOf[JobApplicationActor]), "JobApplicationActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor]), "SessionActor3")

  val createJobApplication = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val clientData = json.toString()
        val jobApplication = extractEntity[JobApplication](clientData)
        val result = ask(jobApplicationActor, jobApplication).mapTo[Future[Boolean]]
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

  val saveToClientConsole = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val clientData = json.toString()
        val command = extractEntity[StatusCommand](clientData)
        val result = ask(jobApplicationActor, command).mapTo[Future[List[JobApplication]]]
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

  def findAllCandidates(consoleId: String,reference:String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(jobApplicationActor, ConsoleCommand(reference,consoleId)).mapTo[Future[List[JobApplicationView]]]
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

  def findAllApplication(userId: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(jobApplicationActor, FindByIdCommand(BSONObjectID.parse(userId).get)).mapTo[Future[List[RecruiterView]]]
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

  val findAllJobApplications = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(jobApplicationActor, FindAllCommand).mapTo[Future[List[JobApplicationView]]]
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

  def findApplicationByCandidate(jobId:String,candidateId: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(jobApplicationActor, JobApplicationCommand(jobId,candidateId)).mapTo[Future[List[JobApplication]]]
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

  def approveApplication(id:String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val data = json.toString()
        val application = extractEntity[JobApplication](data)
        val updateCommand = UserUpdateCommand[JobApplication](BSONObjectID.parse(id).get,application)
        val result = ask(jobApplicationActor, updateCommand).mapTo[Future[List[JobApplication]]]
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
}
