package com.cvcloud.client.controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import com.cvcloud.client.services._
import com.cvcloud.client.utils.BaseAuthentication._
import com.cvcloud.client.utils.JsonImplicits._
import com.cvcloud.client.utils._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Action, Controller, Session => _}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Donald Pollock on 12/07/2017.
  */
@Singleton
class InterviewController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val interviewActor = creator.createActorRef(Props(classOf[InterviewActor]), "InterviewActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor]), "SessionActor233")

  val createInterview = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val jsonData = json.toString()
        val interview = extractEntity[InterviewCommand](jsonData)
        val result = ask(interviewActor, interview).mapTo[Future[Boolean]]
        val actualResult = result.flatMap(identity)
        actualResult.map { act =>
          if (act) Ok(toJson(StatusMessage("Interview Schedule Created")))
          else Ok(toJson(StatusMessage("Interview Schedule Not Created")))
        }
      }
      else {
        Future(Ok(toJson(StatusMessage("User Session expired"))))
      }
    })
  }

  def getInterviewsByCandidate(candidateId: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(interviewActor, candidateId).mapTo[Future[List[InterViewSchedule]]]
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

  def getInterviewsById(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(interviewActor, FindByIdCommand(BSONObjectID.parse(id).get)).mapTo[Future[List[InterViewSchedule]]]
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

  def getInterviewsByClientId(clientId: String,jobId:String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(interviewActor, InterviewClientCommand(clientId,jobId)).mapTo[Future[List[InterViewScheduleClientView]]]
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

  val acceptInterview = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val jsonData = json.toString()
        val interview = extractEntity[InterViewScheduleView](jsonData)
        val updateCommand = UserUpdateCommand[InterViewScheduleView](BSONObjectID.parse(interview._id).get, interview)
        val result = ask(interviewActor, updateCommand).mapTo[Future[List[InterViewSchedule]]]
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

  def uploadFile(random: String) = Action.async(parse.multipartFormData) { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val file: Option[FilePart[TemporaryFile]] = request.body.file("file")
        val size = file.get.ref.file.length()
        if (size / 1024 > Constants.FILE_SIZE) {
          Future {
            Ok(toJson(StatusMessage("File size is too big")))
          }
        }
        else {
          val command = RandomCommand(file, random,DocumentConstant)
          val result = ask(interviewActor, command).mapTo[Future[FormPart]]
          val actualResult = result.flatMap(identity)
          actualResult.map { res =>
            Ok(toJson(res))
          }
        }
      }
      else {
        Future(Ok(toJson(StatusMessage("User Session expired"))))
      }
    })
  }

  def uploadPhoto(random: String) = Action.async(parse.multipartFormData) { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val file: Option[FilePart[TemporaryFile]] = request.body.file("file")
        val size = file.get.ref.file.length()
        if (size / 1024 > Constants.FILE_SIZE) {
          Future {
            Ok(toJson(StatusMessage("File size is too big")))
          }
        }
        else {
          val command = RandomCommand(file, random,PhotoConstant)
          val result = ask(interviewActor, command).mapTo[Future[FormPart]]
          val actualResult = result.flatMap(identity)
          actualResult.map { res =>
            Ok(toJson(res))
          }
        }
      }
      else {
        Future(Ok(toJson(StatusMessage("User Session expired"))))
      }
    })
  }
}
