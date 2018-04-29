package com.cvcloud.client.services

import java.io.File
import java.util.Date

import akka.actor.Actor
import com.cvcloud.client.utils.CVCCalenderColumnConstants.{ID => _, ISREMOVED => _, _}
import com.cvcloud.client.utils.InterviewColumnConstants._
import com.cvcloud.client.utils.JsonImplicits._
import com.cvcloud.client.utils._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Donald Pollock on 23/06/2017.
  */

case class InterViewSchedule(override val _id: String, clientId: String, jobId: String, organiser: Organiser,
                             candidateId: String, interviewInfo: String, interviewType: String, schedules: List[CVCCalender],
                             considerSalaryRange: Boolean, minSalary: Double, maxSalary: Option[Double],
                             acceptCommute: Boolean, jobLocation: String, interviewAddress: String,
                             sharingDocuments: List[Documents], duration: Double, hasAccepted: Boolean, permanent: Boolean) extends BaseEntity

case class Organiser(name: String, position: String, location: String, linkedinUrl: String, photo: Option[String])

case class CVCCalender(dateTime: Date, isSelected: Boolean)

case class InterViewScheduleView(override val _id: String, client: Option[CVCClient], job: Option[Job], organiser: Organiser,
                                 candidateId: String, interviewInfo: String, interviewType: String, schedules: List[CVCCalender],
                                 considerSalaryRange: Boolean, minSalary: Double, maxSalary: Option[Double],
                                 acceptCommute: Boolean, jobLocation: String, interviewAddress: String,
                                 sharingDocuments: List[Documents], duration: Double, hasAccepted: Boolean, permanent: Boolean) extends BaseEntity

case class InterViewScheduleClientView(override val _id: String, clientId: String, jobId: String, organiser: Organiser,
                                 candidate: Option[ShortCandidate], interviewInfo: String, interviewType: String, schedules: List[CVCCalender],
                                 considerSalaryRange: Boolean, minSalary: Double, maxSalary: Option[Double],
                                 acceptCommute: Boolean, jobLocation: String, interviewAddress: String,
                                 sharingDocuments: List[Documents], duration: Double, hasAccepted: Boolean, permanent: Boolean) extends BaseEntity

object InterViewSchedule {

  implicit object OrganiserReader extends BSONDocumentReader[Organiser] {
    def read(doc: BSONDocument): Organiser = {
      val name = doc.getAs[String](NAME).get
      val position = doc.getAs[String](POSITION).get
      val location = doc.getAs[String](LOCATION).get
      val linkedinUrl = doc.getAs[String](LINKEDIN_URL).get
      val photo = doc.getAs[BSONObjectID](PHOTO)
      val actualPhoto = if (photo.isDefined) Some(photo.get.stringify) else None
      Organiser(name, position, location, linkedinUrl, actualPhoto)
    }
  }

  implicit object CVCCalenderReader extends BSONDocumentReader[CVCCalender] {
    def read(doc: BSONDocument): CVCCalender = {
      val dateTime = doc.getAs[Date](DATE_TIME).get
      val isSelected = doc.getAs[Boolean](IS_SELECTED).get
      CVCCalender(dateTime, isSelected)
    }
  }

  implicit object DocumentsReader extends BSONDocumentReader[Documents] {
    def read(doc: BSONDocument): Documents = {
      val name = doc.getAs[String](NAME).get
      val objectId = doc.getAs[BSONObjectID](OBJECT_ID).get
      val description = doc.getAs[String](DESCRIPTION).get
      Documents(name, objectId.stringify, description)
    }
  }

  implicit object InterViewScheduleReader extends BSONDocumentReader[InterViewSchedule] {
    def read(doc: BSONDocument): InterViewSchedule = {
      val id = doc.getAs[BSONObjectID](ID).get
      val clientId = doc.getAs[BSONObjectID](CLIENT_ID).get
      val jobId = doc.getAs[BSONObjectID](JOB_ID).get
      val organiser = doc.getAs[Organiser](ORGANISER).get
      val candidateId = doc.getAs[BSONObjectID](CANDIDATE_ID).get
      val interviewInfo = doc.getAs[String](INTERVIEW_INFO).get
      val interviewType = doc.getAs[String](INTERVIEW_TYPE).get
      val schedules = doc.getAs[List[CVCCalender]](SCHEDULES).get
      val considerSalaryRange = doc.getAs[Boolean](CONSIDER_SALARY_RANGE).get
      val minSalary = doc.getAs[Double](MIN_SALARY).get
      val maxSalary = doc.getAs[Double](MAX_SALARY)
      val acceptCommute = doc.getAs[Boolean](ACCEPT_COMMUTE).get
      val jobLocation = doc.getAs[String](JOB_LOCATION).get
      val interviewAddress = doc.getAs[String](INTERVIEW_ADDRESS).get
      val sharingDocuments = doc.getAs[List[Documents]](SHARING_DOCUMENTS).get
      val duration = doc.getAs[Double](SLOT_DURATION).get
      val hasAccepted = doc.getAs[Boolean](HAS_ACCEPTED).get
      val permanent = doc.getAs[Boolean](PERMANENT).get

      InterViewSchedule(id.stringify, clientId.stringify, jobId.stringify, organiser, candidateId.stringify, interviewInfo,
        interviewType, schedules, considerSalaryRange, minSalary, maxSalary, acceptCommute, jobLocation, interviewAddress, sharingDocuments,
        duration, hasAccepted, permanent)
    }
  }

  implicit object CVCCalenderWriter extends BSONDocumentWriter[CVCCalender] {
    def write(calender: CVCCalender): BSONDocument = {
      BSONDocument(
        DATE_TIME -> calender.dateTime,
        IS_SELECTED -> calender.isSelected
      )
    }
  }

  implicit object InterViewScheduleWriter extends BSONDocumentWriter[InterViewSchedule] {
    def write(interview: InterViewSchedule): BSONDocument = {
      val id = BSONObjectID.generate()
      val clientId = BSONObjectID.parse(interview.clientId).get
      val jobId = BSONObjectID.parse(interview.jobId).get
      val candidateId = BSONObjectID.parse(interview.candidateId).get
      val photo = if (interview.organiser.photo.isDefined) Some(BSONObjectID.parse(interview.organiser.photo.get).get) else None
      BSONDocument(
        ID -> id,
        CLIENT_ID -> clientId,
        JOB_ID -> jobId,
        ORGANISER -> BSONDocument(NAME -> interview.organiser.name, POSITION -> interview.organiser.position,
          LOCATION -> interview.organiser.location, LINKEDIN_URL -> interview.organiser.linkedinUrl, PHOTO -> photo),
        CANDIDATE_ID -> candidateId,
        INTERVIEW_INFO -> interview.interviewInfo,
        INTERVIEW_TYPE -> interview.interviewType,
        SCHEDULES -> interview.schedules,
        CONSIDER_SALARY_RANGE -> interview.considerSalaryRange,
        MIN_SALARY -> interview.minSalary,
        MAX_SALARY -> interview.maxSalary,
        ACCEPT_COMMUTE -> interview.acceptCommute,
        JOB_LOCATION -> interview.jobLocation,
        INTERVIEW_ADDRESS -> interview.interviewAddress,
        SHARING_DOCUMENTS -> interview.sharingDocuments.map(x => BSONDocument(NAME -> x.name,
          OBJECT_ID -> BSONObjectID.parse(x.objectId).get, DESCRIPTION -> x.description)),
        SLOT_DURATION -> interview.duration,
        HAS_ACCEPTED -> interview.hasAccepted,
        PERMANENT -> interview.permanent,
        ISREMOVED -> interview.isRemoved
      )
    }
  }

}

class InterviewRepository extends BaseRepository[InterViewSchedule] with RestService {
  override def table: String = Constants.CVC_INTERVIEW

  var documentMap: Map[String, List[Documents]] = Map.empty
  var photoMap: Map[String, String] = Map.empty

  def saveInterview(cmd: InterviewCommand): Future[Boolean] = {
    val url: String = "http://localhost:9000/createNewsFeed"
    val request = client.url(url)
    val candidateUrl = "http://localhost:9000/userByCandidate/" + cmd.schedule.candidateId
    val candidateRequest = client.url(candidateUrl)
    val feed = NewsFeed("", cmd.schedule.jobId, cmd.schedule.candidateId, "Interview request sent by client.please accept if it is suitable",
      Some(new Date()))
    val documents = documentMap.get(cmd.random)
    val sharedDocuments = if (documents.isDefined) {
      documents.get
    } else Nil
    val photo = photoMap.get(cmd.random)
    val organiser = cmd.schedule.organiser.copy(photo = photo)
    val command = cmd.schedule.copy(sharingDocuments = sharedDocuments, organiser = organiser)
    for {
      saveInterview <- save(command)
      apiKey = JwtUtility.createToken(toJson(Token(TestApiKey.API_KEY)))
      saveFeed <- request.withHeaders("apiKey" -> apiKey).withHeaders("Accept" -> "application/json").post(toJson(feed))
      result <- candidateRequest.withHeaders("apiKey" -> apiKey).withHeaders("Accept" -> "application/json").get()
      user = extractEntity[CVUser](result.body)
      client <- CVCClientRepositoryImpl.findById(BSONObjectID.parse(cmd.schedule.clientId).get)
      consoleId = client.head.consoles.filter(_.jobIds contains cmd.schedule.jobId).head.consoleId
      clientNewsFeed = ClientNewsFeed("",consoleId, cmd.schedule.jobId, cmd.schedule.clientId,
        s"Interview request sent to ${user.firstName} ${user.lastName}:is pending to be accepted", Some(new Date()))
      saveClientFeed <- ClientNewsFeedRepositoryImpl.save(clientNewsFeed)
    } yield saveInterview
  }

  def acceptRequest(cmd: InterViewScheduleView): Future[List[InterViewSchedule]] = {
    import InterViewSchedule._

    val candidateUrl = "http://localhost:9000/userByCandidate/" + cmd.candidateId
    val candidateRequest = client.url(candidateUrl)
    val document = BSONDocument("$set" -> BSONDocument(HAS_ACCEPTED -> cmd.hasAccepted, SCHEDULES -> cmd.schedules))
    for {
      accept <- updateById(BSONObjectID.parse(cmd._id).get, document)
      apiKey = JwtUtility.createToken(toJson(Token(TestApiKey.API_KEY)))
      result <- candidateRequest.withHeaders("apiKey" -> apiKey).withHeaders("Accept" -> "application/json").get()
      user = extractEntity[CVUser](result.body)
      clientNewsFeed = ClientNewsFeed("",cmd.job.get.consoleId, cmd.job.get._id, cmd.client.get._id,
        s"Interview request sent to ${user.firstName} ${user.lastName}:has been accepted", Some(new Date()))
      saveClientFeed <- ClientNewsFeedRepositoryImpl.save(clientNewsFeed)
    } yield accept
  }

  def getInterviewsForCandidate(candidateId: String): Future[List[InterViewScheduleView]] = {
    for {
      interviews <- filterQuery(BSONDocument(CANDIDATE_ID -> BSONObjectID.parse(candidateId).get))
      jobs <- JobRepositoryImpl.getJobsByIds(interviews.map(_.jobId))
      clients <- CVCClientRepositoryImpl.getClietsByIds(interviews.map(_.clientId))
      result = interviews.map(interview => InterViewScheduleView(interview._id, getResult[CVCClient](clients, interview.clientId),
        getResult[Job](jobs, interview.jobId), interview.organiser, interview.candidateId, interview.interviewInfo, interview.interviewType,
        interview.schedules, interview.considerSalaryRange, interview.minSalary, interview.maxSalary, interview.acceptCommute, interview.jobLocation,
        interview.interviewAddress, interview.sharingDocuments, interview.duration, interview.hasAccepted, interview.permanent))
    } yield result
  }

  def getInterviewsForClient(cmd: InterviewClientCommand): Future[List[InterViewScheduleClientView]] = {
    val url: String = "http://localhost:9000/getUsersByCandidateIds"
    val request = client.url(url)
    for {
      interviews <- filterQuery(BSONDocument(CLIENT_ID -> BSONObjectID.parse(cmd.clientId).get,
        JOB_ID -> BSONObjectID.parse(cmd.jobId).get))
      apiKey = JwtUtility.createToken(toJson(Token(TestApiKey.API_KEY)))
      candidateIds = interviews.map(x=>BSONObjectID.parse(x.candidateId).get)
      response <- request.withHeaders("apiKey" -> apiKey).withHeaders("Accept" -> "application/json").post(toJson(candidateIds))
      candidates = extractEntity[List[ShortCandidate]](response.body)
      result = interviews.map(interview => InterViewScheduleClientView(interview._id, interview.clientId,
        interview.jobId, interview.organiser, getUserByCandidate(candidates,interview.candidateId), interview.interviewInfo, interview.interviewType,
        interview.schedules, interview.considerSalaryRange, interview.minSalary, interview.maxSalary, interview.acceptCommute, interview.jobLocation,
        interview.interviewAddress, interview.sharingDocuments, interview.duration, interview.hasAccepted, interview.permanent))
    } yield result
  }

  def getResult[T <: BaseEntity](list: List[T], id: String): Option[T] = {
    list.find(l => l._id == id)
  }

  def getUserByCandidate(list: List[ShortCandidate], candidateId: String): Option[ShortCandidate] = {
    list.find(l => l.candidateId == candidateId)
  }

  def saveDocumentsForInterview(cmd: RandomCommand): Future[FormPart] = {
    val filename = cmd.file.get.filename
    val staticFile = new File(s"$filename")
    val file = cmd.file.get.ref.moveTo(staticFile, true)
    for {
      saveToDb <- saveFileToDb(file)
      someData = extractEntity[FormPart](toJson(saveToDb))
      result = if (documentMap.get(cmd.random).isDefined) {
        val list = documentMap.get(cmd.random).get ::: List(Documents(filename, Helper.convertToObjectId(someData.id.raw), filename + " Document"))
        documentMap += (cmd.random -> list)
      } else {
        val list = List(Documents(filename, Helper.convertToObjectId(someData.id.raw), filename + " Document"))
        documentMap += (cmd.random -> list)
      }
      deleteFile = staticFile.delete()
    } yield someData
  }

  def savePhotoForInterview(cmd: RandomCommand): Future[FormPart] = {
    val filename = cmd.file.get.filename
    val staticFile = new File(s"$filename")
    val file = cmd.file.get.ref.moveTo(staticFile, true)
    for {
      saveToDb <- saveFileToDb(file)
      someData = extractEntity[FormPart](toJson(saveToDb))
      result = if (photoMap.get(cmd.random).isDefined) {
        photoMap -= cmd.random
        val photo = Helper.convertToObjectId(someData.id.raw)
        photoMap += (cmd.random -> photo)
      } else {
        val photo = Helper.convertToObjectId(someData.id.raw)
        photoMap += (cmd.random -> photo)
      }
      deleteFile = staticFile.delete()
    } yield someData
  }
}

object InterviewRepositoryImpl extends InterviewRepository

class InterviewActor extends Actor {
  override def receive: Receive = {
    case FindAllCommand => sender ! InterviewRepositoryImpl.findAll()
    case cmd: InterviewCommand => sender ! InterviewRepositoryImpl.saveInterview(cmd)
    case cmd: FindByIdCommand => sender ! InterviewRepositoryImpl.findById(cmd.id)
    case cmd: UserUpdateCommand[InterViewScheduleView] => sender ! InterviewRepositoryImpl.acceptRequest(cmd.user)
    case candidateId: String => sender ! InterviewRepositoryImpl.getInterviewsForCandidate(candidateId)
    case cmd: InterviewClientCommand => sender ! InterviewRepositoryImpl.getInterviewsForClient(cmd)

    case command: RandomCommand => {
      command.constant match {
        case DocumentConstant => sender ! InterviewRepositoryImpl.saveDocumentsForInterview(command)
        case PhotoConstant => sender ! InterviewRepositoryImpl.savePhotoForInterview(command)
      }
    }
  }
}

case class RandomCommand(file: Option[FilePart[TemporaryFile]], random: String, constant: FileConstant)

case class InterviewCommand(schedule: InterViewSchedule, random: String)

case object PhotoConstant extends FileConstant

case object DocumentConstant extends FileConstant

case class InterviewClientCommand(clientId: String, jobId: String)

case class ShortCandidate(candidateId: String, firstName: String, lastName: String)