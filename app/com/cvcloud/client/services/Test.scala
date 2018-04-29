package com.cvcloud.client.services

import com.cvcloud.client.utils.RestService
import com.cvcloud.client.utils.JsonImplicits._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Donald Pollock on 27/10/2017.
  */
object Test extends RestService {
  def getNews(): Future[FinalResult] = {
    val url1 = "http://localhost:9000/createNewsFeed"
    val request1 = client.url(url1)
    val url2 = "http://localhost:9000/createNewsFeed"
    val request2 = client.url(url2)
    //from cache
    val tokenFromCache = "token"
    val sdsds: Future[FinalResult] = for {
      secondResult <- request2.withHeaders("apiKey" -> tokenFromCache).withHeaders("Accept" -> "application/json").get()
      result = if (secondResult.status != 200) {
        val result = request1.withHeaders("Accept" -> "application/json").get()
        val finalRes = result.flatMap(res => {
          val response = extractEntity[TokenResponse](res.body)
          request2.withHeaders("apiKey" -> response.token).withHeaders("Accept" -> "application/json").get()
        })
        finalRes
      } else {
        Future(secondResult)
      }
      futureRes <- result
      response = extractEntity[FinalResult](futureRes.body)
    } yield response
    sdsds
  }

}

case class TokenResponse(token: String)

case class FinalResult(result: String)