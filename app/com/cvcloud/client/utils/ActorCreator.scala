package com.cvcloud.client.utils

import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout

import scala.concurrent.Await

/**
  * Created by DDM on 24-04-2017.
  */
@Singleton
class ActorCreator @Inject()(system: ActorSystem) {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val ref = system.actorOf(Props[ActorSupervisor])

  def createRouter(props: Props, name: String): ActorRef = {
    val future = ask(ref, ActorName(props, name)).mapTo[ActorRef]
    Await.result(future, Constants.TIMEOUT)
  }

  def createActorRef(props: Props, name: String): ActorRef = {
    createRouter(RoundRobinPool(1).props(props), name)
  }
}

class ActorSupervisor extends Actor {
  override def receive: Receive = {
    case cmd: ActorName => sender ! context.actorOf(cmd.props, cmd.name)
  }
}

case class ActorName(props: Props, name: String)