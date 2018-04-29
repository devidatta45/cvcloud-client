package com.cvcloud.client.services

/**
  * Created by DDM on 20-04-2017.
  */

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import com.cvcloud.client.utils.ClientColumnConstants._
import com.cvcloud.client.utils._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import JsonImplicits._

case class CVUser(override val _id: String, title: String, firstName: String, lastName: String,
                  email: String, password: String, clientRef: Option[String],
                  mobile: String, address: String, country: String, postCode: String) extends BaseEntity

case class Login(email: String, password: String)

case class LoginWithSession(login: Login, actorRef: ActorRef)

case class LoginResult(user: CVUser, token: Option[String])

//Companion object for Mapping
object CVUser {

  implicit object UserReader extends BSONDocumentReader[CVUser] {
    def read(doc: BSONDocument): CVUser = {
      val id = doc.getAs[BSONObjectID](ID).get
      val title = doc.getAs[String](TITLE).get
      val firstName = doc.getAs[String](FIRST_NAME).get
      val lastName = doc.getAs[String](LAST_NAME).get
      val userName = doc.getAs[String](EMAIL).get
      val password = doc.getAs[String](PASSWORD).get
      val clientRef = doc.getAs[String](CLIENT_REF_NUMBER)
      val mobile = doc.getAs[String](MOBILE).get
      val address = doc.getAs[String](ADDRESS).get
      val country = doc.getAs[String](COUNTRY).get
      val postCode = doc.getAs[String](POST_CODE).get
      CVUser(id.stringify, title, firstName, lastName, userName, password, clientRef, mobile, address, country, postCode)
    }
  }

  implicit object UserWriter extends BSONDocumentWriter[CVUser] {
    def write(user: CVUser): BSONDocument = {
      val id = BSONObjectID.generate()
      BSONDocument(ID -> id,
        TITLE -> user.title,
        FIRST_NAME -> user.firstName,
        LAST_NAME -> user.lastName,
        EMAIL -> user.email,
        PASSWORD -> user.password,
        CLIENT_REF_NUMBER -> user.clientRef,
        MOBILE -> user.mobile,
        ADDRESS -> user.address,
        COUNTRY -> user.country,
        POST_CODE -> user.postCode,
        ISREMOVED -> user.isRemoved)
    }
  }

}

//Repository Based on Model. if any extra db related method required apart from Base that can be done here
class CVUserRepository extends BaseRepository[CVUser] {
  override def table: String = Constants.USER_TABLE
}

//Implementation Object for Repository
object CVUserRepositoryImpl extends CVUserRepository

//Service for Data Model. If any server side logic will be there that will be done here.
class CVUserActor extends Actor {

  def updateUser(cmd: UserUpdateCommand[CVUser]): Unit = {
    val document = BSONDocument(
      "$set" -> BSONDocument(
        TITLE -> cmd.user.title,
        FIRST_NAME -> cmd.user.firstName,
        LAST_NAME -> cmd.user.lastName,
        EMAIL -> cmd.user.email,
        PASSWORD -> cmd.user.password,
        MOBILE -> cmd.user.mobile,
        ADDRESS -> cmd.user.address,
        COUNTRY -> cmd.user.country,
        POST_CODE -> cmd.user.postCode))
    sender ! CVUserRepositoryImpl.updateById(cmd.id, document)
  }

  def checkAuthentication(loginWithSession: LoginWithSession): Unit = {
    import akka.pattern.ask
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._
    val document = BSONDocument(EMAIL -> loginWithSession.login.email, PASSWORD -> loginWithSession.login.password)
    val uuid = UUID.randomUUID().toString
    val token = JwtUtility.createToken(toJson(Token(uuid)))
    val sessionResult = for {
      result <- CVUserRepositoryImpl.filterQuery(document)
      finalResult <- if (result.nonEmpty && result.head.clientRef.isDefined && result.head.clientRef.get.equals(result.head.password)) {
        val session = Session("", result.head._id, uuid)
        val res = ask(loginWithSession.actorRef, session)(5.seconds).mapTo[Future[Boolean]]
        res.flatMap(identity)
      } else Future(false)
      loginResult = if (finalResult) {
        LoginResult(result.head, Some(token))
      } else LoginResult(result.headOption.getOrElse(CVUser("", "", "", "", "", "", None, "", "", "", "")), None)
    } yield loginResult
    sender ! sessionResult
  }

  override def receive: Receive = {
    case FindAllCommand => sender ! CVUserRepositoryImpl.findAll()
    //will check if the client reference key is present using admin module rest end point(once admin module is up and running)
    case user: CVUser => sender ! CVUserRepositoryImpl.save(user)
    case login: LoginWithSession => checkAuthentication(login)
    case cmd: FindByIdCommand => sender ! CVUserRepositoryImpl.findById(cmd.id)
    case cmd: DeleteByIdCommand => sender ! CVUserRepositoryImpl.deleteById(cmd.id)
    case cmd: UserUpdateCommand[CVUser]@unchecked => updateUser(cmd)
  }
}