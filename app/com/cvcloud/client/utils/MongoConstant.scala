package com.cvcloud.client.utils

import com.typesafe.config.ConfigFactory
import reactivemongo.bson.BSONObjectID

/**
  * Created by DDM on 13-04-2017.
  */

// Db Constants
object MongoConstant {
  val config = ConfigFactory.load
  val server = config.getString("mongo.url")
  val dbName = config.getString("mongo.dbname")
  val userName = config.getString("mongo.username")
  val password = config.getString("mongo.password")
}

//generic classes and objects
case object FindAllCommand

case class UserUpdateCommand[T](id: BSONObjectID, user: T)

case class FindByIdCommand(id: BSONObjectID)

case class DeleteByIdCommand(id: BSONObjectID)