package com.cvcloud.client.services

import akka.actor.Actor
import com.cvcloud.client.utils.CVCSkillColumnConstants._
import com.cvcloud.client.utils._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

/**
  * Created by Donald Pollock on 20/06/2017.
  */
case class CVCSkill(override val _id: String, name: String) extends BaseEntity

object CVCSkill {

  implicit object CVCSkillReader extends BSONDocumentReader[CVCSkill] {
    def read(doc: BSONDocument): CVCSkill = {
      val id = doc.getAs[BSONObjectID](ID).get
      val name = doc.getAs[String](NAME).get
      CVCSkill(id.stringify, name)
    }
  }

  implicit object CVCSkillWriter extends BSONDocumentWriter[CVCSkill] {
    def write(skill: CVCSkill): BSONDocument = {
      val id = BSONObjectID.generate()
      BSONDocument(
        ID -> id,
        NAME -> skill.name,
        ISREMOVED -> skill.isRemoved
      )
    }
  }

}

class CVCSkillRepository extends BaseRepository[CVCSkill] {
  override def table: String = Constants.CVC_SKILLS
}

object CVCSkillRepositoryImpl extends CVCSkillRepository

class CVCSkillActor extends Actor {
  override def receive: Receive = {
    case FindAllCommand => sender ! CVCSkillRepositoryImpl.findAll()
    case cmd: CVCSkill => sender ! CVCSkillRepositoryImpl.save(cmd)
  }
}