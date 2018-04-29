package com.cvcloud.client.services

import java.util.Date

import akka.actor.Actor
import com.cvcloud.client.utils.JobApplicationColumnConstants._
import com.cvcloud.client.utils.JsonImplicits._
import com.cvcloud.client.utils._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Donald Pollock on 08/06/2017.
  */
case class JobApplication(override val _id: String, jobId: String, candidateId: String, userId: String, candidateSkills: List[Skill],
                          adminSkills: List[Skill], approvedSkills: List[Skill], qualifiedRatio: String, jobStatus: String, isVerified: Boolean) extends BaseEntity

case class JobApplicationView(override val _id: String, jobId: String, candidate: Option[CVCCandidate], user: Option[CVUser], candidateSkills: List[Skill],
                              adminSkills: List[Skill], approvedSkills: List[Skill], qualifiedRatio: String, jobStatus: String, isVerified: Boolean) extends BaseEntity

object JobApplication {

  implicit object SkillReader extends BSONDocumentReader[Skill] {
    def read(doc: BSONDocument): Skill = {
      val name = doc.getAs[String](ClientColumnConstants.NAME).get
      val score = doc.getAs[Int](ClientColumnConstants.SCORE).get
      val isPrimary = doc.getAs[Boolean](ClientColumnConstants.IS_PRIMARY).get
      Skill(name, score, isPrimary)
    }
  }

  implicit object JobApplicationReader extends BSONDocumentReader[JobApplication] {
    def read(doc: BSONDocument): JobApplication = {
      val id = doc.getAs[BSONObjectID](ID).get
      val jobId = doc.getAs[BSONObjectID](JOB_ID).get
      val candidateId = doc.getAs[BSONObjectID](CANDIDATE_ID).get
      val userId = doc.getAs[BSONObjectID](USER_ID).get
      val candidateSkills = doc.getAs[List[Skill]](CANDIDATE_SKILLS).get
      val adminSkills = doc.getAs[List[Skill]](ADMIN_SKILLS).get
      val approvedSkills = doc.getAs[List[Skill]](APPROVED_SKILLS).get
      val qualifiedRatio = doc.getAs[String](QUALIFIED_RATIO).get
      val jobStatus = doc.getAs[String](JOB_STATUS).get
      val isVerified = doc.getAs[Boolean](IS_VERIFIED).get
      JobApplication(id.stringify, jobId.stringify, candidateId.stringify, userId.stringify, candidateSkills, adminSkills, approvedSkills, qualifiedRatio, jobStatus, isVerified)
    }
  }

  implicit object JobApplicationWriter extends BSONDocumentWriter[JobApplication] {
    def write(jobApplication: JobApplication): BSONDocument = {
      val id = BSONObjectID.generate()
      val jobId = BSONObjectID.parse(jobApplication.jobId).get
      val candidateId = BSONObjectID.parse(jobApplication.candidateId).get
      val userId = BSONObjectID.parse(jobApplication.userId).get
      BSONDocument(ID -> id,
        JOB_ID -> jobId,
        CANDIDATE_ID -> candidateId,
        USER_ID -> userId,
        CANDIDATE_SKILLS -> jobApplication.candidateSkills.map(x => BSONDocument(ClientColumnConstants.NAME -> x.name,
          ClientColumnConstants.SCORE -> x.score, ClientColumnConstants.IS_PRIMARY -> x.isPrimary)),
        ADMIN_SKILLS -> jobApplication.adminSkills.map(x => BSONDocument(ClientColumnConstants.NAME -> x.name,
          ClientColumnConstants.SCORE -> x.score, ClientColumnConstants.IS_PRIMARY -> x.isPrimary)),
        APPROVED_SKILLS -> jobApplication.approvedSkills.map(x => BSONDocument(ClientColumnConstants.NAME -> x.name,
          ClientColumnConstants.SCORE -> x.score, ClientColumnConstants.IS_PRIMARY -> x.isPrimary)),
        QUALIFIED_RATIO -> jobApplication.qualifiedRatio,
        JOB_STATUS -> jobApplication.jobStatus,
        IS_VERIFIED -> jobApplication.isVerified,
        ISREMOVED -> jobApplication.isRemoved)
    }
  }

}

class JobApplicationRepository extends BaseRepository[JobApplication] with RestService {
  override def table: String = Constants.JOB_APPLICATION_TABLE

  def saveJobApplication(cmd: JobApplication): Future[Boolean] = {
    val feed = NewsFeed("", cmd.jobId, cmd.candidateId, cmd.jobStatus, Some(new Date()))
    val url: String = "http://localhost:9000/createNewsFeed"
    val request = client.url(url)
    for {
      application <- save(cmd)
      apiKey = JwtUtility.createToken(toJson(Token(TestApiKey.API_KEY)))
      saveFeed <- request.withHeaders("apiKey" -> apiKey).withHeaders("Accept" -> "application/json").post(toJson(feed))
    } yield application
  }

  def saveCandidateToClientConsole(command: StatusCommand) = {
    val url: String = "http://localhost:9000/createNewsFeed"
    val request = client.url(url)
    val feed = NewsFeed("", command.jobId, command.candidateId, command.message + " by client", Some(new Date()))
    for {
      filteredApplication <- filterQuery(BSONDocument(JOB_ID -> BSONObjectID.parse(command.jobId).get,
        CANDIDATE_ID -> BSONObjectID.parse(command.candidateId).get))
      document = BSONDocument("$set" -> BSONDocument(JOB_STATUS -> command.message))
      result <- updateById(BSONObjectID.parse(filteredApplication.head._id).get, document)
      apiKey = JwtUtility.createToken(toJson(Token(TestApiKey.API_KEY)))
      saveFeed <- request.withHeaders("apiKey" -> apiKey).withHeaders("Accept" -> "application/json").post(toJson(feed))
    } yield result
  }

  def findCandidatesByConsoleId(consoleId: String,clientRefNumber:String): Future[List[JobApplicationView]] = {
    val url: String = "http://localhost:9000/getUsersByIds"
    val clientUrl: String = "http://localhost:9000/getCandidatesByIds"
    val request = client.url(url)
    val requestCandidate = client.url(clientUrl)
    for {
      client <- CVCClientRepositoryImpl.filterQuery(BSONDocument(ClientColumnConstants.CLIENT_REF_NUMBER->clientRefNumber))
      jobIds = client.head.consoles.filter(_.consoleId == consoleId).head.jobIds
      filteredResult <- filterQuery(BSONDocument(JOB_ID ->BSONDocument("$in"-> jobIds.map(jobId=>BSONObjectID.parse(jobId).get)),IS_VERIFIED -> true))
      userIds = filteredResult.map(x => BSONObjectID.parse(x.userId).get)
      candidateIds = filteredResult.map(x => BSONObjectID.parse(x.candidateId).get)
      apiKey = JwtUtility.createToken(toJson(Token(TestApiKey.API_KEY)))
      result <- request.withHeaders("apiKey" -> apiKey).withHeaders("Accept" -> "application/json")
        .post(toJson(userIds))
      candidateResult <- requestCandidate.withHeaders("apiKey" -> apiKey).withHeaders("Accept" -> "application/json")
        .post(toJson(candidateIds))
      users = extractEntity[List[CVUser]](result.body)
      candidates = extractEntity[List[CVCCandidate]](candidateResult.body)
      viewResult = filteredResult.map(res => JobApplicationView(res._id, res.jobId, getCandidate[CVCCandidate](candidates, res.candidateId)
        , getCandidate[CVUser](users, res.userId), res.candidateSkills, res.adminSkills, res.approvedSkills, res.qualifiedRatio, res.jobStatus, res.isVerified))
    } yield viewResult
  }

  def findAllCandidates(): Future[List[JobApplicationView]] = {
    val url: String = "http://localhost:9000/getUsersByIds"
    val clientUrl: String = "http://localhost:9000/getCandidatesByIds"
    val request = client.url(url)
    val requestCandidate = client.url(clientUrl)
    for {
      filteredResult <- findAll()
      userIds = filteredResult.map(x => BSONObjectID.parse(x.userId).get)
      candidateIds = filteredResult.map(x => BSONObjectID.parse(x.candidateId).get)
      apiKey = JwtUtility.createToken(toJson(Token(TestApiKey.API_KEY)))
      result <- request.withHeaders("apiKey" -> apiKey).withHeaders("Accept" -> "application/json")
        .post(toJson(userIds))
      candidateResult <- requestCandidate.withHeaders("apiKey" -> apiKey).withHeaders("Accept" -> "application/json")
        .post(toJson(candidateIds))
      users = extractEntity[List[CVUser]](result.body)
      candidates = extractEntity[List[CVCCandidate]](candidateResult.body)
      viewResult = filteredResult.map(res => JobApplicationView(res._id, res.jobId, getCandidate[CVCCandidate](candidates, res.candidateId)
        , getCandidate[CVUser](users, res.userId), res.candidateSkills, res.adminSkills, res.approvedSkills, res.qualifiedRatio, res.jobStatus, res.isVerified))
    } yield viewResult
  }

  def getCandidate[T <: BaseEntity](list: List[T], id: String): Option[T] = {
    list.find(l => l._id == id)
  }

  def getApplicationBasedOnCandidate(userId: BSONObjectID): Future[List[RecruiterView]] = {
    for {
      filteredApplications <- filterQuery(BSONDocument(USER_ID -> userId))
      filteredJobs <- JobRepositoryImpl.getJobsByIds(filteredApplications.map(_.jobId))
      clients <- CVCClientRepositoryImpl.getClietsByIds(filteredJobs.map(_.clientId))
      applications = filteredJobs.map(job => RecruiterView(job.jobTitle, getCandidate[CVCClient](clients, job.clientId).get.companyName, job.startDate))
    } yield applications
  }

  def approveApplication(id: BSONObjectID, application: JobApplication): Future[List[JobApplication]] = {
    val url: String = "http://localhost:9000/createNewsFeed"
    val request = client.url(url)
    val feed = NewsFeed("", application.jobId, application.candidateId, "Application is approved by admin..", Some(new Date()))
    for {
      filteredJob <- JobRepositoryImpl.findById(BSONObjectID.parse(application.jobId).get)
      qualifiedRatio = getQualifiedRatio(filteredJob.head.primarySkillsRequired, application.approvedSkills)
      newApplication = application.copy(qualifiedRatio = qualifiedRatio)
      document = BSONDocument("$set" -> BSONDocument(
        ADMIN_SKILLS -> newApplication.adminSkills.map(x => BSONDocument(ClientColumnConstants.NAME -> x.name,
          ClientColumnConstants.SCORE -> x.score, ClientColumnConstants.IS_PRIMARY -> x.isPrimary)),
        APPROVED_SKILLS -> newApplication.approvedSkills.map(x => BSONDocument(ClientColumnConstants.NAME -> x.name,
          ClientColumnConstants.SCORE -> x.score, ClientColumnConstants.IS_PRIMARY -> x.isPrimary)),
        QUALIFIED_RATIO -> newApplication.qualifiedRatio,
        JOB_STATUS -> newApplication.jobStatus,
        IS_VERIFIED -> newApplication.isVerified))
      updateApplication <- updateById(id, document)
      apiKey = JwtUtility.createToken(toJson(Token(TestApiKey.API_KEY)))
      saveFeed <- request.withHeaders("apiKey" -> apiKey).withHeaders("Accept" -> "application/json").post(toJson(feed))
    } yield updateApplication
  }

  def getQualifiedRatio(requiredSkills: List[Skill], approvedSkills: List[Skill]): String = {
    var qualify = 0
    requiredSkills.foreach { skill =>
      val foundSkill = approvedSkills.find(sk => sk.name == skill.name)
      if (foundSkill.isDefined && foundSkill.get.score >= skill.score) qualify = qualify + 1
    }
    qualify.toString + "/" + requiredSkills.length.toString
  }
}

object JobApplicationRepositoryImpl extends JobApplicationRepository

class JobApplicationActor extends Actor {
  override def receive: Receive = {
    case cmd: JobApplication => sender ! JobApplicationRepositoryImpl.saveJobApplication(cmd)
    case FindAllCommand => sender ! JobApplicationRepositoryImpl.findAllCandidates()
    case cmd: ConsoleCommand => sender ! JobApplicationRepositoryImpl.findCandidatesByConsoleId(cmd.consoleId,cmd.reference)
    case command: StatusCommand => sender ! JobApplicationRepositoryImpl.saveCandidateToClientConsole(command)
    case cmd: FindByIdCommand => sender ! JobApplicationRepositoryImpl.getApplicationBasedOnCandidate(cmd.id)
    case cmd: JobApplicationCommand => sender ! JobApplicationRepositoryImpl.filterQuery(BSONDocument(JOB_ID -> BSONObjectID.parse(cmd.jobId).get,
      CANDIDATE_ID -> BSONObjectID.parse(cmd.candidateId).get))
    case cmd: UserUpdateCommand[JobApplication] => sender ! JobApplicationRepositoryImpl.approveApplication(cmd.id, cmd.user)
  }
}

case class NewsFeed(_id: String, jobId: String, candidateId: String, message: String, date: Option[Date])

case class CVCCandidate(override val _id: String, cvcUserId: String, photo: Option[String],
                        education: List[Education], skills: List[Skill], createdBy: Audit, modifiedBy: Audit, currentStatus: Option[String],
                        employmentDetails: Option[CVCCandidateEmploymentDetails]) extends BaseEntity

case class CVCCandidateEmploymentDetails(currentEmployer: Option[Employer], previousEmployers: List[Employer],
                                         jobLocations: List[String], jobTravelDistancePerm: Option[Double],
                                         jobTravelDistanceContract: Option[Double], noticePeriod: Option[Int], telephonicNotice: Option[Double],
                                         faceToFaceNotice: Option[Double], currentSalary: Option[Double], contractRate: Option[Double],
                                         requiredSalary: Option[Double], requiredContractRate: Option[Double], isNegotiable: Option[Boolean],
                                         previousClientRef: List[Reference], newJobReason: Option[String],
                                         linkedinUrl: Option[String], resume: Option[Resume], candidateDocuments: List[Documents],
                                         cvCloudDocuments: List[Documents], permanent: Option[Boolean], contract: Option[Boolean])

case class Employer(name: String, startDate: Option[Date], endDate: Option[Date],
                    jobTitle: String, salary: Double, location: String, skills: List[Skill])

case class Education(degree: String, completionYear: String)

case class Reference(name: String, mobile: String, email: String)

case class Resume(version: String, objectId: String)

case class Documents(name: String, objectId: String, description: String)

case class StatusCommand(jobId: String, candidateId: String, message: String)

case class JobApplicationCommand(jobId: String, candidateId: String)

case class RecruiterView(jobTitle: String, client: String, startDate: Option[Date])