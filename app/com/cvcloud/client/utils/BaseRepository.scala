package com.cvcloud.client.utils

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.concurrent.duration._

import play.api.libs.Files.TemporaryFile
import play.api.libs.iteratee.Enumerator
import play.api.mvc.MultipartFormData.FilePart
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteConcern
import reactivemongo.api.gridfs.Implicits._
import reactivemongo.api.gridfs.{DefaultFileToSave, GridFS, ReadFile}
import reactivemongo.api.{BSONSerializationPack, _}
import reactivemongo.bson._
import BaseColumnConstants._
import reactivemongo.core.nodeset.Authenticate

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Created by DDM on 19-04-2017.
  */

//All Model should extend this class
trait BaseEntity {
  val _id: String
  val isRemoved: Boolean = false
}

trait BaseRepository[T <: BaseEntity] {

  import MongoConstant._

  import ExecutionContext.Implicits.global

  def table: String

  val driver = new MongoDriver
  val credentials = List(Authenticate(dbName, userName, password))
  val connection = Try {
    driver.connection(List(server),authentications = credentials, options = MongoConnectionOptions(
      readPreference = ReadPreference.primary,
      writeConcern = WriteConcern.Default,
      authMode = ScramSha1Authentication
    ))
  }
  val futureConnection = Future.fromTry(connection)
  val db = futureConnection.flatMap(_.database(dbName,FailoverStrategy(100.milliseconds, 20, {n => n})))
  val collection: Future[BSONCollection] = db.map(_.collection[BSONCollection](table))
  val chunkCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("fs.chunks"))
  type BSONFile = reactivemongo.api.gridfs.ReadFile[BSONSerializationPack.type, BSONValue]
  type BSONGridFS = GridFS[BSONSerializationPack.type]

  val cacheMap = new mutable.HashMap[String, List[T]]()

  def resolveGridFS(db: DefaultDB): BSONGridFS = GridFS(db)

  //All the generic methods for db manipulation required for any kind of operations for any data Model

  def findAll()(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    val query = BSONDocument(ISREMOVED -> false)
    filterQuery(query)
  }

  def save(t: T)(implicit writer: BSONDocumentWriter[T], reader: BSONDocumentReader[T]): Future[Boolean] = {
    val query = BSONDocument(ISREMOVED -> false)
    for {
      document <- collection.flatMap(_.insert(t).map(x => x.ok))
      updatedList <- filterQuery(query)
      updateCache = cacheMap.put(table, updatedList)
    } yield document
  }

  def findById(id: BSONObjectID)(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    val query = BSONDocument(ID -> id, ISREMOVED -> false)
    filterQuery(query)
  }

  def filterQuery(document: BSONDocument)(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    val newDocument = document ++ BSONDocument(ISREMOVED -> false)
    collection.flatMap(_.find(newDocument).cursor[T]().collect[List]())
  }

  def filterQueryGridfs(document: BSONDocument)(implicit reader: BSONDocumentReader[ActualFileData]): Future[List[ActualFileData]] = {
    chunkCollection.flatMap(_.find(document).cursor[ActualFileData]().collect[List]())
  }

  def updateById(id: BSONObjectID, document: BSONDocument)(implicit writer: BSONDocumentWriter[T], reader: BSONDocumentReader[T]): Future[List[T]] = {
    val selector = BSONDocument(ID -> id, ISREMOVED -> false)
    for {
      updateDocument <- collection.flatMap(_.update(selector, document).map(_.n))
      updatedUser <- findById(id)
    } yield updatedUser
  }

  def deleteById(id: BSONObjectID)(implicit writer: BSONDocumentWriter[T]): Future[Int] = {
    val selector = BSONDocument(ID -> id, ISREMOVED -> false)
    val modifier = BSONDocument("$set" -> BSONDocument(ISREMOVED -> true))
    for {
      document <- collection.flatMap(_.update(selector, modifier).map(_.n))
    } yield document
  }

  def saveFileToDb(file: File): Future[BSONFile] = {
    val enumerator = Enumerator.fromFile(file)
    val result = for {
      d <- db
      res <- saveToGridFS(resolveGridFS(d), file.getName, Some(Constants.FILE_TYPE), enumerator)
    } yield res
    result
  }

  def getFileFromDb(id: BSONObjectID): Future[ReadFile[BSONSerializationPack.type, BSONValue]] = {
    val result = for {
      d <- db
      res <- gridfsByFilename(resolveGridFS(d), id)
    } yield res
    result
  }

  def downloadFile(id: BSONObjectID, fileName: String, userId: String)(implicit reader: BSONDocumentReader[ActualFileData]): Future[Path] = {
    val document = BSONDocument("files_id" -> id)
    for {
      finalResult <- filterQueryGridfs(document)
      result = if (Files.notExists(Paths.get(Constants.FILE_PATH + userId))) {
        Files.createDirectory(Paths.get(Constants.FILE_PATH + userId))
        Files.write(Paths.get(Constants.FILE_PATH + userId + "/" + fileName), finalResult.head.data)
      } else if (!Helper.checkFilePresentInFolder(Constants.FILE_PATH + userId, fileName)) {
        Files.write(Paths.get(Constants.FILE_PATH + userId + "/" + fileName), finalResult.head.data)
      } else Paths.get(Constants.FILE_PATH + userId + "/" + fileName)
    } yield result
  }

  def removeFileFromDb(id: BSONValue): Future[Unit] = {
    val result = for {
      d <- db
      res <- removeFrom(resolveGridFS(d), id)
    } yield res
    result
  }

  private def saveToGridFS(gridfs: GridFS[BSONSerializationPack.type], filename: String, contentType: Option[String],
                           data: Enumerator[Array[Byte]])(implicit ec: ExecutionContext): Future[BSONFile] = {
    val gridfsObj = DefaultFileToSave(Some(filename), contentType)
    gridfs.save(data, gridfsObj)
  }

  private def gridfsByFilename(gridfs: GridFS[BSONSerializationPack.type], id: BSONObjectID)
                              (implicit ec: ExecutionContext): Future[ReadFile[BSONSerializationPack.type, BSONValue]] = {
    def cursor = gridfs.find(BSONDocument(ID -> id))
    cursor.head
  }

  private def removeFrom(gridfs: GridFS[BSONSerializationPack.type], id: BSONValue)
                        (implicit ec: ExecutionContext): Future[Unit] = gridfs.remove(id).map(_ => {})
}

case class FormPart(id: Raw, contentType: String, filename: String)

case class ActualPart(id: String, contentType: String, filename: String, fileType: String)

case class Raw(raw: List[Int])

case class ActualFileData(_id: BSONObjectID, files_id: BSONObjectID, n: Int, data: Array[Byte])

object ActualFileData {

  implicit object ActualFileDataReader extends BSONDocumentReader[ActualFileData] {
    def read(doc: BSONDocument): ActualFileData = {
      val id = doc.getAs[BSONObjectID]("_id").get
      val files_id = doc.getAs[BSONObjectID]("files_id").get
      val n = doc.getAs[Int]("n").get
      val data = doc.getAs[Array[Byte]]("data").get
      ActualFileData(id, files_id, n, data)
    }
  }

}

case class UserIdCommand(id: BSONObjectID)

case class UpdateFileCommand(id: String, consoleId: String, file: Option[FilePart[TemporaryFile]], constant: FileConstant)

case class FileCommand(id: BSONObjectID)

case class DownloadCommand(actualPart: ActualPart, userId: String)

case class Token(key:String)

trait FileConstant

case object DOCUMENTCONSTANT extends FileConstant