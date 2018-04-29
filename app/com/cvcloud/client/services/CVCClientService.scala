package com.cvcloud.client.services

import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.security.SecureRandom
import java.util.{Date, UUID}

import akka.actor.Actor
import com.cvcloud.client.utils._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}
import ClientColumnConstants._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import JsonImplicits._

/**
  * Created by Donald Pollock on 31/05/2017.
  */
case class CVCClient(override val _id: String, clientRefNumber: String, registration: String, vat: String,
                     companyName: String, companyAddress: String, postCode: String, county: String,
                     telephone: String, website: String, primaryContact: PrimaryContact, approval: Option[Approval],
                     noOfConsoles: Option[Int], consoles: List[Console], totalPayment: Option[Payment], createdBy: Audit,
                     modifiedBy: Audit) extends BaseEntity

case class CVCClientView(override val _id: String, clientRefNumber: String, registration: String, vat: String,
                         companyName: String, companyAddress: String, postCode: String, county: String,
                         telephone: String, website: String, primaryContact: PrimaryContact, approval: Option[Approval],
                         noOfConsoles: Option[Int], consoles: List[ConsoleView], totalPayment: Option[Payment], createdBy: Audit,
                         modifiedBy: Audit) extends BaseEntity

case class Approval(status: String, code: String, financeContact: PrimaryContact)

case class PrimaryContact(firstName: String, lastName: String, mobile: String, email: String, jobTitle: String)

case class Payment(plan: String, totalCost: Double, totalPaid: Double, totalDue: Double, method: String,
                   paidDate: Option[Date], dueDate: Option[Date])

case class Console(consoleId: String, name: String, jobIds: List[String], documents: List[ConsoleDocument], skills: List[Skill])

case class ConsoleView(consoleId: String, name: String, jobIds: List[Job], documents: List[ConsoleDocument], skills: List[Skill])

case class Skill(name: String, score: Int, isPrimary: Boolean)

case class Audit(userId: String, date: Option[Date] = Some(new Date()))

case class ConsoleDocument(name: String, objectId: String, documentType: String,
                           author: String, modifiedDate: Option[Date] = Some(new Date()))

object CVCClient {

  implicit object AuditReader extends BSONDocumentReader[Audit] {
    def read(doc: BSONDocument): Audit = {
      val userId = doc.getAs[BSONObjectID](USER_ID).get
      val date = doc.getAs[Date](DATE)
      Audit(userId.stringify, date)
    }
  }

  implicit object ConsoleDocumentReader extends BSONDocumentReader[ConsoleDocument] {
    def read(doc: BSONDocument): ConsoleDocument = {
      val name = doc.getAs[String](NAME).get
      val objectId = doc.getAs[BSONObjectID](OBJECT_ID).get
      val documentType = doc.getAs[String](DOCUMENT_TYPE).get
      val author = doc.getAs[String](AUTHOR).get
      val modifiedDate = doc.getAs[Date](MODIFIED_DATE)
      ConsoleDocument(name, objectId.stringify, documentType, author, modifiedDate)
    }
  }

  implicit object SkillReader extends BSONDocumentReader[Skill] {
    def read(doc: BSONDocument): Skill = {
      val name = doc.getAs[String](ClientColumnConstants.NAME).get
      val score = doc.getAs[Int](ClientColumnConstants.SCORE).get
      val isPrimary = doc.getAs[Boolean](ClientColumnConstants.IS_PRIMARY).get
      Skill(name, score, isPrimary)
    }
  }

  implicit object ConsoleReader extends BSONDocumentReader[Console] {
    def read(doc: BSONDocument): Console = {
      val consoleId = doc.getAs[BSONObjectID](CONSOLE_ID).get
      val name = doc.getAs[String](NAME).get
      val jobIds = doc.getAs[List[BSONObjectID]](JOB_IDS).get.map(_.stringify)
      val documents = doc.getAs[List[ConsoleDocument]](CONSOLE_DOCUMENTS).get
      val skills = doc.getAs[List[Skill]](SKILLS).get
      Console(consoleId.stringify, name, jobIds, documents, skills)
    }
  }

  implicit object PrimaryContactReader extends BSONDocumentReader[PrimaryContact] {
    def read(doc: BSONDocument): PrimaryContact = {
      val firstName = doc.getAs[String](FIRST_NAME).get
      val lastName = doc.getAs[String](LAST_NAME).get
      val mobile = doc.getAs[String](MOBILE).get
      val email = doc.getAs[String](EMAIL).get
      val jobTitle = doc.getAs[String](JOB_TITLE).get
      PrimaryContact(firstName, lastName, mobile, email, jobTitle)
    }
  }

  implicit object ApprovalReader extends BSONDocumentReader[Approval] {
    def read(doc: BSONDocument): Approval = {
      val code = doc.getAs[String](CODE).get
      val status = doc.getAs[String](STATUS).get
      val financeContact = doc.getAs[PrimaryContact](FINANCE_CONTACT).get
      Approval(status, code, financeContact)
    }
  }

  implicit object PaymentReader extends BSONDocumentReader[Payment] {
    def read(doc: BSONDocument): Payment = {
      val plan = doc.getAs[String](PLAN).get
      val totalCost = doc.getAs[Double](TOTAL_COST).get
      val totalPaid = doc.getAs[Double](TOTAL_PAID).get
      val totalDue = doc.getAs[Double](TOTAL_DUE).get
      val method = doc.getAs[String](METHOD).get
      val paidDate = doc.getAs[Date](PAID_DATE)
      val dueDate = doc.getAs[Date](DUE_DATE)
      Payment(plan, totalCost, totalPaid, totalDue, method, paidDate, dueDate)
    }
  }

  implicit object CVCClientReader extends BSONDocumentReader[CVCClient] {
    def read(doc: BSONDocument): CVCClient = {
      val id = doc.getAs[BSONObjectID](ID).get
      val clientRefNumber = doc.getAs[String](CLIENT_REF_NUMBER).get
      val registration = doc.getAs[String](REGISTRATION).get
      val vat = doc.getAs[String](VAT).get
      val companyName = doc.getAs[String](COMPANY_NAME).get
      val companyAddress = doc.getAs[String](COMPANY_ADDRESS).get
      val postCode = doc.getAs[String](POST_CODE).get
      val county = doc.getAs[String](COUNTY).get
      val telephone = doc.getAs[String](TELEPHONE).get
      val website = doc.getAs[String](WEBSITE).get
      val primaryContact = doc.getAs[PrimaryContact](PRIMARY_CONTACT).get
      val approval = doc.getAs[Approval](APPROVAL)
      val noOfConsoles = doc.getAs[Int](NO_OF_CONSOLES)
      val consoles = doc.getAs[List[Console]](CONSOLES).get
      val totalPayment = doc.getAs[Payment](TOTAL_PAYMENT)
      val createdBy = doc.getAs[Audit](CREATED_BY).get
      val modifiedBy = doc.getAs[Audit](MODIFIED_BY).get
      CVCClient(id.stringify, clientRefNumber, registration, vat, companyName, companyAddress, postCode, county,
        telephone, website, primaryContact, approval, noOfConsoles, consoles, totalPayment, createdBy, modifiedBy)
    }
  }

  implicit object ConsoleDocumentWriter extends BSONDocumentWriter[ConsoleDocument] {
    def write(document: ConsoleDocument): BSONDocument = {
      val objectId = BSONObjectID.parse(document.objectId).get
      BSONDocument(NAME -> document.name,
        OBJECT_ID -> objectId,
        DOCUMENT_TYPE -> document.documentType,
        AUTHOR -> document.author,
        MODIFIED_DATE -> document.modifiedDate)
    }
  }

  implicit object ConsoleWriter extends BSONDocumentWriter[Console] {
    def write(document: Console): BSONDocument = {
      val consoleId = BSONObjectID.parse(document.consoleId).get
      val jobIds = document.jobIds.map(id => BSONObjectID.parse(id).get)
      BSONDocument(CONSOLE_ID -> consoleId,
        NAME -> document.name,
        JOB_IDS -> jobIds,
        CONSOLE_DOCUMENTS -> document.documents,
        SKILLS -> document.skills.map(x => BSONDocument(NAME -> x.name, SCORE -> x.score, IS_PRIMARY -> x.isPrimary))
      )
    }
  }

  implicit object PrimaryContactWriter extends BSONDocumentWriter[PrimaryContact] {
    def write(document: PrimaryContact): BSONDocument = {
      BSONDocument(FIRST_NAME -> document.firstName,
        LAST_NAME -> document.lastName, MOBILE -> document.mobile,
        EMAIL -> document.email, JOB_TITLE -> document.jobTitle)
    }
  }

  implicit object ApprovalWriter extends BSONDocumentWriter[Approval] {
    def write(document: Approval): BSONDocument = {
      BSONDocument(STATUS -> document.status, CODE -> document.code,
        FINANCE_CONTACT -> document.financeContact)
    }
  }

  implicit object PaymentWriter extends BSONDocumentWriter[Payment] {
    def write(document: Payment): BSONDocument = {
      BSONDocument(PLAN -> document.plan, TOTAL_COST -> document.totalCost,
        TOTAL_PAID -> document.totalPaid, TOTAL_DUE -> document.totalDue,
        METHOD -> document.method, PAID_DATE -> document.paidDate, DUE_DATE -> document.dueDate)
    }
  }

  implicit object CVCClientWriter extends BSONDocumentWriter[CVCClient] {
    def write(client: CVCClient): BSONDocument = {
      val id = BSONObjectID.generate()
      BSONDocument(ID -> id,
        CLIENT_REF_NUMBER -> client.clientRefNumber,
        REGISTRATION -> client.registration,
        VAT -> client.vat,
        COMPANY_NAME -> client.companyName,
        COMPANY_ADDRESS -> client.companyAddress,
        POST_CODE -> client.postCode,
        COUNTY -> client.county,
        TELEPHONE -> client.telephone,
        WEBSITE -> client.website,
        PRIMARY_CONTACT -> client.primaryContact,
        APPROVAL -> client.approval,
        NO_OF_CONSOLES -> client.noOfConsoles,
        CONSOLES -> client.consoles,
        TOTAL_PAYMENT -> client.totalPayment,
        CREATED_BY -> BSONDocument(USER_ID -> BSONObjectID.parse(client.createdBy.userId).get, DATE -> client.createdBy.date),
        MODIFIED_BY -> BSONDocument(USER_ID -> BSONObjectID.parse(client.modifiedBy.userId).get, DATE -> client.modifiedBy.date),
        ISREMOVED -> client.isRemoved)
    }
  }

}

class CVCClientRepository extends BaseRepository[CVCClient] {

  import CVCClient._

  override def table: String = Constants.CLIENT

  def saveClient(client: CVCClient): Future[List[CVCClientView]] = {
    val reference = RandomGenerator.nextId
    val clientWithReference = client.copy(clientRefNumber = reference)
    for {
      newClient <- save(clientWithReference)
      document = BSONDocument(CLIENT_REF_NUMBER -> clientWithReference.clientRefNumber)
      result <- filterQuery(document)
      user = CVUser("", "Mr.", client.primaryContact.firstName, client.primaryContact.lastName,
        client.primaryContact.email, reference, Some(reference), client.primaryContact.mobile, client.companyAddress,
        client.county, client.postCode)
      saveUser <- CVUserRepositoryImpl.save(user)
      allJobs <- JobRepositoryImpl.getJobsByIds(result.flatMap(_.consoles.flatMap(_.jobIds)))
      finalResult = result.map(res => CVCClientView(res._id, res.clientRefNumber, res.registration, res.vat, res.companyName,
        res.companyAddress, res.postCode, res.county, res.telephone, res.website, res.primaryContact, res.approval, res.noOfConsoles,
        getConsoleView(allJobs, res.consoles), res.totalPayment, res.createdBy, res.modifiedBy))
    } yield finalResult
  }

  def getClietsByIds(ids: List[String]): Future[List[CVCClient]] = {
    val document = BSONDocument(ID -> BSONDocument("$in" -> ids.map(id => BSONObjectID.parse(id).get)))
    filterQuery(document)
  }

  def updateClient(id: BSONObjectID, client: CVCClient): Future[List[CVCClient]] = {
    val document = BSONDocument(
      "$set" -> BSONDocument(
        CLIENT_REF_NUMBER -> client.clientRefNumber,
        COMPANY_NAME -> client.companyName,
        COMPANY_ADDRESS -> client.companyAddress,
        PRIMARY_CONTACT -> client.primaryContact,
        CONSOLES -> client.consoles,
        CREATED_BY -> BSONDocument(USER_ID -> BSONObjectID.parse(client.createdBy.userId).get, DATE -> client.createdBy.date),
        MODIFIED_BY -> BSONDocument(USER_ID -> BSONObjectID.parse(client.modifiedBy.userId).get, DATE -> client.modifiedBy.date)
      ))
    updateById(id, document)
  }

  def getByReferenceKey(clientRef: String): Future[List[CVCClientView]] = {
    val document = BSONDocument(CLIENT_REF_NUMBER -> clientRef)
    for {
      clients <- filterQuery(document)
      allJobs <- JobRepositoryImpl.getJobsByIds(clients.flatMap(_.consoles.flatMap(_.jobIds)))
      finalResult = clients.map(res => CVCClientView(res._id, res.clientRefNumber, res.registration, res.vat, res.companyName,
        res.companyAddress, res.postCode, res.county, res.telephone, res.website, res.primaryContact, res.approval, res.noOfConsoles,
        getConsoleView(allJobs, res.consoles), res.totalPayment, res.createdBy, res.modifiedBy))
    } yield finalResult
  }

  def getConsoleView(jobs: List[Job], consoles: List[Console]): List[ConsoleView] = {
    consoles.map(console => {
      val res = jobs.filter(job =>console.jobIds contains job._id)
      ConsoleView(console.consoleId, console.name, res, console.documents, console.skills)
    })
  }

  def updateAttribute(cmd: UpdateFileCommand, func: (Console, CVCClient, ActualPart) => BSONDocument): Future[List[CVCClient]] = {
    import CVCClient._
    val filename = cmd.file.get.filename
    val staticFile = new File(s"$filename")
    val file = cmd.file.get.ref.moveTo(staticFile, true)
    val fileType = Files.probeContentType(file.toPath)
    val result = for {
      saveToDb <- saveFileToDb(file)
      someData = extractEntity[FormPart](toJson(saveToDb))
      actualPart = ActualPart(Helper.convertToObjectId(someData.id.raw), someData.contentType, someData.filename, fileType)
      client <- findById(BSONObjectID.parse(cmd.id).get)
      console = client.head.consoles.find(x => x.consoleId == cmd.consoleId)
      document = func(console.get, client.head, actualPart)
      deleteFile = staticFile.delete()
      updateInDb <- updateById(BSONObjectID.parse(cmd.id).get, document)
    } yield updateInDb
    result
  }

  def updateApproval(command: ApprovalReference): Future[List[CVCClientView]] = {
    val approval = if (command.approval.status == "") command.approval.copy(status = "APPROVED") else command.approval
    for {
      client <- filterQuery(BSONDocument(CLIENT_REF_NUMBER -> command.reference))
      document = BSONDocument("$set" -> BSONDocument(APPROVAL -> Some(approval)))
      updateRecord <- updateById(BSONObjectID.parse(client.head._id).get, document)
      allJobs <- JobRepositoryImpl.getJobsByIds(updateRecord.flatMap(_.consoles.flatMap(_.jobIds)))
      finalResult = updateRecord.map(res => CVCClientView(res._id, res.clientRefNumber, res.registration, res.vat, res.companyName,
        res.companyAddress, res.postCode, res.county, res.telephone, res.website, res.primaryContact, res.approval, res.noOfConsoles,
        getConsoleView(allJobs, res.consoles), res.totalPayment, res.createdBy, res.modifiedBy))
    } yield finalResult
  }

  def updatePayment(command: PaymentReference): Future[List[CVCClientView]] = {
    for {
      client <- filterQuery(BSONDocument(CLIENT_REF_NUMBER -> command.reference))
      document = BSONDocument("$set" -> BSONDocument(TOTAL_PAYMENT -> Some(command.payment)))
      updateRecord <- updateById(BSONObjectID.parse(client.head._id).get, document)
      allJobs <- JobRepositoryImpl.getJobsByIds(updateRecord.flatMap(_.consoles.flatMap(_.jobIds)))
      finalResult = updateRecord.map(res => CVCClientView(res._id, res.clientRefNumber, res.registration, res.vat, res.companyName,
        res.companyAddress, res.postCode, res.county, res.telephone, res.website, res.primaryContact, res.approval, res.noOfConsoles,
        getConsoleView(allJobs, res.consoles), res.totalPayment, res.createdBy, res.modifiedBy))
    } yield finalResult
  }

  def updateConsole(command: ConsoleReference): Future[List[CVCClientView]] = {
    val consoles = 1 to command.noOfConsoles map { cns =>
      val consoleId = BSONObjectID.generate()
      Console(consoleId.stringify, s"CONSOLE-$cns", Nil, Nil, Nil)
    }
    for {
      client <- filterQuery(BSONDocument(CLIENT_REF_NUMBER -> command.reference))
      document = BSONDocument("$set" -> BSONDocument(NO_OF_CONSOLES -> Some(command.noOfConsoles), CONSOLES -> consoles.toList))
      updateRecord <- updateById(BSONObjectID.parse(client.head._id).get, document)
      allJobs <- JobRepositoryImpl.getJobsByIds(updateRecord.flatMap(_.consoles.flatMap(_.jobIds)))
      finalResult = updateRecord.map(res => CVCClientView(res._id, res.clientRefNumber, res.registration, res.vat, res.companyName,
        res.companyAddress, res.postCode, res.county, res.telephone, res.website, res.primaryContact, res.approval, res.noOfConsoles,
        getConsoleView(allJobs, res.consoles), res.totalPayment, res.createdBy, res.modifiedBy))
    } yield finalResult
  }

  def updateConsoleSkills(command: ConsoleSkillsReference): Future[List[CVCClientView]] = {
    for {
      client <- filterQuery(BSONDocument(CLIENT_REF_NUMBER -> command.reference))
      consoles = client.head.consoles.filterNot(_.consoleId == command.consoleId) ::: List(command.console)
      document = BSONDocument("$set" -> BSONDocument(CONSOLES -> consoles))
      updateRecord <- updateById(BSONObjectID.parse(client.head._id).get, document)
      allJobs <- JobRepositoryImpl.getJobsByIds(updateRecord.flatMap(_.consoles.flatMap(_.jobIds)))
      finalResult = updateRecord.map(res => CVCClientView(res._id, res.clientRefNumber, res.registration, res.vat, res.companyName,
        res.companyAddress, res.postCode, res.county, res.telephone, res.website, res.primaryContact, res.approval, res.noOfConsoles,
        getConsoleView(allJobs, res.consoles), res.totalPayment, res.createdBy, res.modifiedBy))
    } yield finalResult
  }

  def getJobsByConsoleId(command: ConsoleCommand): Future[List[Job]] = {
    for {
      filteredClient <- filterQuery(BSONDocument(CLIENT_REF_NUMBER -> command.reference))
      console = filteredClient.head.consoles.filter(_.consoleId == command.consoleId)
      result = if (console.head.jobIds.nonEmpty) {
        JobRepositoryImpl.getJobsByIds(console.head.jobIds)
      } else Future(Nil)
      finalResult <- result
    } yield finalResult
  }
}

object CVCClientRepositoryImpl extends CVCClientRepository

class CVCClientActor extends Actor {

  import CVCClient._

  def documentFunc(console: Console, client: CVCClient, actualPart: ActualPart): BSONDocument = {
    val documents = console.documents ::: List(ConsoleDocument(actualPart.filename, actualPart.id, actualPart.fileType,
      client.companyName, Some(new Date())))
    val newConsoles = client.consoles.filterNot(c => c.consoleId == console.consoleId)
    val consoles = newConsoles ::: List(console.copy(documents = documents))
    val updatedDocument = BSONDocument(
      "$set" -> BSONDocument(
        CONSOLES -> consoles.map(cns => BSONDocument(CONSOLE_ID -> BSONObjectID.parse(cns.consoleId).get,
          NAME -> cns.name, JOB_IDS -> cns.jobIds.map(id => BSONObjectID.parse(id).get), CONSOLE_DOCUMENTS -> cns.documents))))

    updatedDocument
  }

  override def receive: Receive = {
    case FindAllCommand => sender ! CVCClientRepositoryImpl.findAll()
    case cmd: CVCClient => sender ! CVCClientRepositoryImpl.saveClient(cmd)
    case cmd: FindByIdCommand => sender ! CVCClientRepositoryImpl.findById(cmd.id)
    case cmd: DeleteByIdCommand => sender ! CVCClientRepositoryImpl.deleteById(cmd.id)
    case refKey: String => sender ! CVCClientRepositoryImpl.getByReferenceKey(refKey)
    case cmd: UpdateFileCommand => sender ! CVCClientRepositoryImpl.updateAttribute(cmd, documentFunc)
    case cmd: DownloadCommand => {
      sender ! CVCClientRepositoryImpl.downloadFile(BSONObjectID.parse(cmd.actualPart.id).get, cmd.actualPart.filename, cmd.userId)
    }
    case command: ApprovalReference => sender ! CVCClientRepositoryImpl.updateApproval(command)
    case command: PaymentReference => sender ! CVCClientRepositoryImpl.updatePayment(command)
    case command: ConsoleReference => sender ! CVCClientRepositoryImpl.updateConsole(command)
    case command: ConsoleSkillsReference => sender ! CVCClientRepositoryImpl.updateConsoleSkills(command)
    case command: ConsoleCommand => sender ! CVCClientRepositoryImpl.getJobsByConsoleId(command)
  }
}

object RandomGenerator {
  val random = new SecureRandom()

  def nextId: String = {
    val id = new BigInteger(30, random).toString(16)
    "CVC-" + id
  }

  def nextJobId: String = {
    val id = new BigInteger(30, random).toString(16)
    id
  }
}

case class ApprovalReference(reference: String, approval: Approval)

case class PaymentReference(reference: String, payment: Payment)

case class ConsoleReference(reference: String, noOfConsoles: Int)

case class ConsoleSkillsReference(reference: String, consoleId: String, console: Console)

case class ConsoleCommand(reference: String, consoleId: String)