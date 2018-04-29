package com.cvcloud.client.controllers

import java.nio.file.Path
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
import play.api.mvc.{Action, Controller, MultipartFormData, Session => _, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Donald Pollock on 31/05/2017.
  */
@Singleton
class CVCClientController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val clientActor = creator.createActorRef(Props(classOf[CVCClientActor]), "CVCClientActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor]), "SessionActor1")

  val createClient = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val clientData = json.toString()
        val client = extractEntity[CVCClient](clientData)
        val result = ask(clientActor, client).mapTo[Future[List[CVCClientView]]]
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

  val updateApproval = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val clientData = json.toString()
        val approval = extractEntity[ApprovalReference](clientData)
        val result = ask(clientActor, approval).mapTo[Future[List[CVCClientView]]]
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

  val updatePayment = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val clientData = json.toString()
        val approval = extractEntity[PaymentReference](clientData)
        val result = ask(clientActor, approval).mapTo[Future[List[CVCClientView]]]
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

  val updateConsole = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val clientData = json.toString()
        val approval = extractEntity[ConsoleReference](clientData)
        val result = ask(clientActor, approval).mapTo[Future[List[CVCClientView]]]
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

  val updateConsoleSkills = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val clientData = json.toString()
        val approval = extractEntity[ConsoleSkillsReference](clientData)
        val result = ask(clientActor, approval).mapTo[Future[List[CVCClientView]]]
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
  val getTest = Action.async { request =>
    val result = Future(StatusMessage("Hope to get client final result here"))
    result.map { res =>
      Ok(toJson(res))
    }
  }
  val getAllClients = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(clientActor, FindAllCommand).mapTo[Future[List[CVCClient]]]
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

  def getClientByRef(refKey: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(clientActor, refKey).mapTo[Future[List[CVCClientView]]]
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

  def getJobsByConsole(consoleId: String, reference: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(clientActor, ConsoleCommand(reference, consoleId)).mapTo[Future[List[Job]]]
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

  def uploadDocument(id: String, consoleId: String) = Action.async(parse.multipartFormData) { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    uploadFile(id, consoleId, authResult, DOCUMENTCONSTANT, request, "Client Document")
  }

  def uploadFile(id: String, consoleId: String, authResult: Future[List[Session]], fileConstant: FileConstant,
                 request: Request[MultipartFormData[TemporaryFile]], message: String): Future[Result] = {
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
          val command = UpdateFileCommand(id, consoleId, file, fileConstant)
          val result = ask(clientActor, command).mapTo[Future[List[CVCClient]]]
          val actualResult = result.flatMap(identity)
          actualResult.map { res =>
            if (res.nonEmpty) {
              Ok(toJson(StatusMessage(message + " added")))
            }
            else {
              Ok(toJson(StatusMessage(message + " not added")))
            }
          }
        }
      }
      else {
        Future(Ok(toJson(StatusMessage("User Session expired"))))
      }
    })
  }

  val download = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val wholeData = extractEntity[DownloadCommand](message)
        val result = ask(clientActor, wholeData).mapTo[Future[Path]]
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

case class StatusMessage(status: String)