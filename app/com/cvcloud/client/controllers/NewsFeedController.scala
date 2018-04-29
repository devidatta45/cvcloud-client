package com.cvcloud.client.controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import com.cvcloud.client.services.{ClientNewsFeedActor, SessionActor, _}
import com.cvcloud.client.utils.BaseAuthentication._
import com.cvcloud.client.utils.JsonImplicits._
import com.cvcloud.client.utils.{ActorCreator, Constants}
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Donald Pollock on 14/07/2017.
  */
@Singleton
class NewsFeedController @Inject()(creator: ActorCreator) extends Controller{
  implicit val timeout: Timeout = Constants.TIMEOUT
  val newsFeedActor = creator.createActorRef(Props(classOf[ClientNewsFeedActor]), "ClientNewsFeedActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor]), "SessionActor211")

  def getAllNews(clientId: String,consoleId:String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(newsFeedActor, NewsCommand(clientId,consoleId)).mapTo[Future[List[ClientNewsFeed]]]
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
