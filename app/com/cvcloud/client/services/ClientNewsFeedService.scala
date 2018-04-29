package com.cvcloud.client.services

import java.util.Date

import com.cvcloud.client.utils.{BaseEntity, BaseRepository, ClientNewsFeedColumnConstants, Constants}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}
import ClientNewsFeedColumnConstants._
import akka.actor.Actor

/**
  * Created by Donald Pollock on 14/07/2017.
  */

case class ClientNewsFeed(override val _id: String, consoleId: String,jobId:String, clientId: String, message: String, date: Option[Date]) extends BaseEntity

object ClientNewsFeed {

  implicit object NewFeedReader extends BSONDocumentReader[ClientNewsFeed] {
    def read(doc: BSONDocument): ClientNewsFeed = {
      val id = doc.getAs[BSONObjectID](ID).get
      val consoleId = doc.getAs[BSONObjectID](CONSOLE_ID).get
      val jobId = doc.getAs[BSONObjectID](JOB_ID).get
      val clientId = doc.getAs[BSONObjectID](CLIENT_ID).get
      val message = doc.getAs[String](MESSAGE).get
      val date = doc.getAs[Date](DATE)
      ClientNewsFeed(id.stringify, consoleId.stringify,jobId.stringify, clientId.stringify, message, date)
    }
  }

  implicit object NewFeedWriter extends BSONDocumentWriter[ClientNewsFeed] {
    def write(feed: ClientNewsFeed): BSONDocument = {
      val id = BSONObjectID.generate()
      val consoleId = BSONObjectID.parse(feed.consoleId).get
      val jobId = BSONObjectID.parse(feed.jobId).get
      val clientId = BSONObjectID.parse(feed.clientId).get
      BSONDocument(ID -> id,
        CONSOLE_ID -> consoleId,
        CLIENT_ID -> clientId,
        JOB_ID -> jobId,
        MESSAGE -> feed.message,
        DATE -> feed.date,
        ISREMOVED -> feed.isRemoved)
    }
  }

}

class ClientNewsFeedRepository extends BaseRepository[ClientNewsFeed] {
  override def table: String = Constants.CLIENT_NEWS_FEED
}

object ClientNewsFeedRepositoryImpl extends ClientNewsFeedRepository

class ClientNewsFeedActor extends Actor {
  override def receive: Receive = {
    case cmd: ClientNewsFeed => sender ! ClientNewsFeedRepositoryImpl.save(cmd)
    case cmd: NewsCommand => sender ! ClientNewsFeedRepositoryImpl.filterQuery(BSONDocument(CLIENT_ID ->
      BSONObjectID.parse(cmd.clientId).get, CONSOLE_ID -> BSONObjectID.parse(cmd.consoleId).get))
  }
}
case class NewsCommand(clientId: String, consoleId: String)