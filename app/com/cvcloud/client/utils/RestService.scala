package com.cvcloud.client.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient

/**
  * Created by Donald Pollock on 09/06/2017.
  */
trait RestService {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val client = AhcWSClient()
}
