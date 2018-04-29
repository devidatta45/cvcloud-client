package com.cvcloud.client.services

import java.util.Date

import akka.actor.Actor
import com.cvcloud.client.utils._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}
import ClientColumnConstants._
import JobColumnConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Donald Pollock on 31/05/2017.
  */
case class Job(override val _id: String, clientId: String, consoleId: String, jobReference: String, platform: String,
               jobTitle: String, jobAdvert: String, jobLocations: List[String],
               startDate: Option[Date], endDate: Option[Date], isActive: Boolean,
               primarySkillsRequired: List[Skill], secondarySkillsRequired: List[Skill],
               createdBy: Audit, modifiedBy: Audit) extends BaseEntity

object Job {

  implicit object SkillReader extends BSONDocumentReader[Skill] {
    def read(doc: BSONDocument): Skill = {
      val name = doc.getAs[String](NAME).get
      val score = doc.getAs[Int](SCORE).get
      val isPrimary = doc.getAs[Boolean](IS_PRIMARY).get
      Skill(name, score, isPrimary)
    }
  }

  implicit object AuditReader extends BSONDocumentReader[Audit] {
    def read(doc: BSONDocument): Audit = {
      val userId = doc.getAs[BSONObjectID](USER_ID).get
      val date = doc.getAs[Date](DATE)
      Audit(userId.stringify, date)
    }
  }

  implicit object JobReader extends BSONDocumentReader[Job] {
    def read(doc: BSONDocument): Job = {
      val id = doc.getAs[BSONObjectID](JobColumnConstants.ID).get
      val clientId = doc.getAs[BSONObjectID](CLIENT_ID).get
      val consoleId = doc.getAs[BSONObjectID](CONSOLE_ID).get
      val jobReference = doc.getAs[String](JOB_REFERENCE).get
      val platform = doc.getAs[String](PLATFORM).get
      val jobTitle = doc.getAs[String](JobColumnConstants.JOB_TITLE).get
      val jobAdvert = doc.getAs[String](JOB_ADVERT).get
      val jobLocations = doc.getAs[List[String]](JOB_LOCATIONS).get
      val startDate = doc.getAs[Date](START_DATE)
      val endDate = doc.getAs[Date](END_DATE)
      val isActive = doc.getAs[Boolean](IS_ACTIVE).get
      val primarySkillsRequired = doc.getAs[List[Skill]](PRIMARY_SKILLS_REQUIRED).get
      val secondarySkillsRequired = doc.getAs[List[Skill]](SECONDARY_SKILLS_REQUIRED).get
      val createdBy = doc.getAs[Audit](CREATED_BY).get
      val modifiedBy = doc.getAs[Audit](MODIFIED_BY).get
      Job(id.stringify, clientId.stringify, consoleId.stringify, jobReference, platform, jobTitle, jobAdvert, jobLocations, startDate,
        endDate, isActive, primarySkillsRequired, secondarySkillsRequired, createdBy, modifiedBy)
    }
  }

  implicit object JobWriter extends BSONDocumentWriter[Job] {
    def write(job: Job): BSONDocument = {
      val id = BSONObjectID.generate()
      val clientId = BSONObjectID.parse(job.clientId).get
      val consoleId = BSONObjectID.parse(job.consoleId).get
      BSONDocument(JobColumnConstants.ID -> id,
        CLIENT_ID -> clientId,
        CONSOLE_ID -> consoleId,
        JOB_REFERENCE -> job.jobReference,
        PLATFORM -> job.platform,
        JobColumnConstants.JOB_TITLE -> job.jobTitle,
        JOB_ADVERT -> job.jobAdvert,
        JOB_LOCATIONS -> job.jobLocations,
        START_DATE -> job.startDate,
        END_DATE -> job.endDate,
        IS_ACTIVE -> job.isActive,
        PRIMARY_SKILLS_REQUIRED -> job.primarySkillsRequired.map(x => BSONDocument(NAME -> x.name, SCORE -> x.score, IS_PRIMARY -> x.isPrimary)),
        SECONDARY_SKILLS_REQUIRED -> job.secondarySkillsRequired.map(x => BSONDocument(NAME -> x.name, SCORE -> x.score, IS_PRIMARY -> x.isPrimary)),
        CREATED_BY -> BSONDocument(USER_ID -> BSONObjectID.parse(job.createdBy.userId).get, DATE -> job.createdBy.date),
        MODIFIED_BY -> BSONDocument(USER_ID -> BSONObjectID.parse(job.modifiedBy.userId).get, DATE -> job.modifiedBy.date),
        JobColumnConstants.ISREMOVED -> job.isRemoved)
    }
  }

}

class JobRepository extends BaseRepository[Job] {
  override def table: String = Constants.JOBS

  def saveJob(jobCommand: JobCommand): Future[List[CVCClient]] = {
    val reference = RandomGenerator.nextJobId
    val job = jobCommand.job.copy(jobReference = reference)
    for {
      insertJob <- save(job)
      client <- CVCClientRepositoryImpl.filterQuery(BSONDocument(ClientColumnConstants.ID -> BSONObjectID.parse(job.clientId).get))
      doc = BSONDocument(JobColumnConstants.JOB_REFERENCE -> job.jobReference)
      filteredJob <- filterQuery(doc)
      newConsole = jobCommand.console.copy(jobIds = jobCommand.console.jobIds ::: List(filteredJob.head._id))
      alteredConsoles = client.head.consoles.filterNot(x => x.consoleId == jobCommand.console.consoleId)
      list = alteredConsoles ::: List(newConsole)
      res = client.head.copy(consoles = list)
      result <- CVCClientRepositoryImpl.updateClient(BSONObjectID.parse(job.clientId).get, res)
    } yield result
  }

  def getJobsByIds(ids: List[String]): Future[List[Job]] = {
    val document = BSONDocument(JobColumnConstants.ID -> BSONDocument("$in" -> ids.map(id => BSONObjectID.parse(id).get)))
    filterQuery(document)
  }

  def updateJob(jobCommand: JobCommand): Future[List[CVCClient]] = {
    val document = BSONDocument("$set" -> BSONDocument(JobColumnConstants.JOB_TITLE -> jobCommand.job.jobTitle,
      JOB_ADVERT -> jobCommand.job.jobAdvert,
      JOB_LOCATIONS -> jobCommand.job.jobLocations,
      START_DATE -> jobCommand.job.startDate,
      END_DATE -> jobCommand.job.endDate,
      IS_ACTIVE -> jobCommand.job.isActive,
      PRIMARY_SKILLS_REQUIRED -> jobCommand.job.primarySkillsRequired.map(x => BSONDocument(NAME -> x.name, SCORE -> x.score, IS_PRIMARY -> x.isPrimary)),
      SECONDARY_SKILLS_REQUIRED -> jobCommand.job.secondarySkillsRequired.map(x => BSONDocument(NAME -> x.name, SCORE -> x.score, IS_PRIMARY -> x.isPrimary)),
      CREATED_BY -> BSONDocument(USER_ID -> BSONObjectID.parse(jobCommand.job.createdBy.userId).get, DATE -> jobCommand.job.createdBy.date),
      MODIFIED_BY -> BSONDocument(USER_ID -> BSONObjectID.parse(jobCommand.job.modifiedBy.userId).get, DATE -> jobCommand.job.modifiedBy.date)))
    for {
      updateJob <- updateById(BSONObjectID.parse(jobCommand.job._id).get, document)
      client <- CVCClientRepositoryImpl.filterQuery(BSONDocument(ClientColumnConstants.ID -> BSONObjectID.parse(jobCommand.job.clientId).get))
      doc = BSONDocument(CLIENT_ID -> BSONObjectID.parse(jobCommand.job.clientId).get, CONSOLE_ID -> BSONObjectID.parse(jobCommand.job.consoleId).get,
        JobColumnConstants.JOB_TITLE -> jobCommand.job.jobTitle)
      filteredJob <- filterQuery(doc)
      newConsole = jobCommand.console.copy(jobIds = jobCommand.console.jobIds ::: List(filteredJob.head._id))
      alteredConsoles = client.head.consoles.filterNot(x => x.consoleId == jobCommand.console.consoleId)
      list = alteredConsoles ::: List(newConsole)
      res = client.head.copy(consoles = list)
      result <- CVCClientRepositoryImpl.updateClient(BSONObjectID.parse(jobCommand.job.clientId).get, res)
    } yield result
  }

  def updateSkills(cmd: SkillUpdateCommand): Future[List[Job]] = {
    val isPrimary = cmd.skills.head.isPrimary
    val document = if (isPrimary) BSONDocument("$set" -> BSONDocument(PRIMARY_SKILLS_REQUIRED -> cmd.skills.map(x => BSONDocument
    (NAME -> x.name, SCORE -> x.score, IS_PRIMARY -> x.isPrimary))))
    else
      BSONDocument("$set" -> BSONDocument(SECONDARY_SKILLS_REQUIRED -> cmd.skills.map(x => BSONDocument
      (NAME -> x.name, SCORE -> x.score, IS_PRIMARY -> x.isPrimary))))
    updateById(BSONObjectID.parse(cmd.jobId).get, document)
  }
}

object JobRepositoryImpl extends JobRepository

class JobActor extends Actor {
  override def receive: Receive = {
    case FindAllCommand => sender ! JobRepositoryImpl.findAll()
    case cmd: JobCommand => if (cmd.isUpdate) sender ! JobRepositoryImpl.updateJob(cmd) else sender ! JobRepositoryImpl.saveJob(cmd)
    case cmd: JobReferenceCommand => sender ! JobRepositoryImpl.filterQuery(BSONDocument(JOB_REFERENCE -> cmd.reference, IS_ACTIVE -> true))
    case cmd: DeleteByIdCommand => sender ! JobRepositoryImpl.deleteById(cmd.id)
    case cmd: SkillUpdateCommand => sender ! JobRepositoryImpl.updateSkills(cmd)
    case cmd: FindByIdCommand => {
      val document = BSONDocument("$set" -> BSONDocument(IS_ACTIVE -> true))
      sender ! JobRepositoryImpl.updateById(cmd.id, document)
    }
  }
}

case class JobCommand(job: Job, console: Console, isUpdate: Boolean)

case class SkillUpdateCommand(jobId: String, skills: List[Skill])

case class JobReferenceCommand(reference: String)