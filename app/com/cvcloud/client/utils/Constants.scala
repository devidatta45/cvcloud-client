package com.cvcloud.client.utils

import java.io.File

import scala.concurrent.duration._

/**
  * Created by Donald Pollock on 31/05/2017.
  */
object Constants {
  val TIMEOUT = 5.seconds
  val FILE_TYPE = "multipart/form-data"
  val FILE_PATH = "public/files/"
  val FILE_SIZE = 2048
  val CLIENT = "CVCCLIENT"
  val JOBS = "CVCJOBS"
  val USER_TABLE = "CVCUSER"
  val CLIENT_NEWS_FEED = "CVCCLIENTNEWSFEED"
  val JOB_APPLICATION_TABLE = "CVCJOBAPPLICATION"
  val CVC_SKILLS = "CVCSKILLS"
  val CVC_INTERVIEW = "CVCINTERVIEW"
  val SESSION = "SESSION"
}

trait BaseColumnConstants {
  val ID = "_id"
  val ISREMOVED = "isRemoved"
}

object BaseColumnConstants extends BaseColumnConstants

object CVCSkillColumnConstants extends BaseColumnConstants {
  val NAME = "NAME"
}

object CVCCalenderColumnConstants extends BaseColumnConstants {
  val DATE_TIME = "DATETIME"
  val TIME = "TIME"
  val DURATION = "DURATION"
  val IS_SELECTED = "ISSELECTED"
}

object InterviewColumnConstants extends BaseColumnConstants {
  val CLIENT_ID = "CLIENTID"
  val JOB_ID = "JOBID"
  val ORGANISER = "ORGANISER"
  val CANDIDATE_ID = "CANDIDATEID"
  val INTERVIEW_INFO = "INTERVIEWINFO"
  val INTERVIEW_TYPE = "INTERVIEWTYPE"
  val SCHEDULES = "SCHEDULES"
  val CONSIDER_SALARY_RANGE = "CONSIDERSALARYRANGE"
  val MIN_SALARY = "MIN_SALARY"
  val MAX_SALARY = "MAX_SALARY"
  val ACCEPT_COMMUTE = "ACCEPTCOMMUTE"
  val JOB_LOCATION = "JOBLOCATION"
  val INTERVIEW_ADDRESS = "INTERVIEWADDRESS"
  val SHARING_DOCUMENTS = "SHARINGDOCUMENTS"
  val SLOT_DURATION = "SLOTDURATION"
  val HAS_ACCEPTED = "HASACCEPTED"
  val PERMANENT = "PERMANENT"
  val NAME = "NAME"
  val OBJECT_ID = "OBJECT_ID"
  val DESCRIPTION = "DESCRIPTION"
  val POSITION = "POSITION"
  val LOCATION = "LOCATION"
  val LINKEDIN_URL = "LINKEDIN_URL"
  val PHOTO = "PHOTO"
}

object SessionColumnConstants extends BaseColumnConstants {
  val USER_ID = "USERID"
  val TOKEN = "TOKEN"
}

object ClientNewsFeedColumnConstants extends BaseColumnConstants {
  val CONSOLE_ID = "CONSOLEID"
  val JOB_ID = "JOBID"
  val CLIENT_ID = "CLIENTID"
  val MESSAGE = "MESSAGE"
  val DATE = "DATE"
}

object ClientColumnConstants extends BaseColumnConstants {
  val CLIENT_REF_NUMBER = "CLIENTREFNUMBER"
  val REGISTRATION = "REGISTRATION"
  val VAT = "VAT"
  val COMPANY_NAME = "COMPANYNAME"
  val COMPANY_ADDRESS = "COMPANYADDRESS"
  val COUNTY = "COUNTY"
  val TELEPHONE = "TELEPHONE"
  val WEBSITE = "WEBSITE"
  val PRIMARY_CONTACT = "PRIMARYCONTACT"
  val APPROVAL = "APPROVAL"
  val NO_OF_CONSOLES = "NOOFCONSOLES"
  val CONSOLES = "CONSOLES"
  val TOTAL_PAYMENT = "TOTALPAYMENT"
  val USERS = "USERS"
  val CLIENT_ADVERT = "CLIENTADVERT"
  val CREATED_BY = "CREATEDBY"
  val MODIFIED_BY = "MODIFIEDBY"
  val CONSOLE_ID = "CONSOLEID"
  val JOB_IDS = "JOBIDS"
  val CONSOLE_DOCUMENTS = "CONSOLEDOCUMENTS"
  val SKILLS = "SKILLS"
  val NAME = "NAME"
  val SCORE = "SCORE"
  val IS_PRIMARY = "ISPRIMARY"
  val USER_ID = "USERID"
  val ROLE = "ROLE"
  val DATE = "DATE"
  val TITLE = "TITLE"
  val FIRST_NAME = "FIRSTNAME"
  val LAST_NAME = "LASTNAME"
  val CODE = "CODE"
  val STATUS = "STATUS"
  val FINANCE_CONTACT = "FINANCECONTACT"
  val EMAIL = "EMAIL"
  val JOB_TITLE = "JOBTITLE"
  val PASSWORD = "PASSWORD"
  val MOBILE = "MOBILE"
  val ADDRESS = "ADDRESS"
  val COUNTRY = "COUNTRY"
  val POST_CODE = "POSTCODE"
  val OBJECT_ID = "OBJECTID"
  val DOCUMENT_TYPE = "DOCUMENTTYPE"
  val AUTHOR = "AUTHOR"
  val MODIFIED_DATE = "MODIFIEDDATE"
  val TOTAL_COST = "TOTALCOST"
  val PLAN = "PAYMENTPLAN"
  val TOTAL_PAID = "TOTALPAID"
  val TOTAL_DUE = "TOTALDUE"
  val METHOD = "METHOD"
  val DUE_DATE = "DUEDATE"
  val PAID_DATE = "PAIDDATE"
}

object JobColumnConstants extends BaseColumnConstants {
  val CLIENT_ID = "CLIENTID"
  val JOB_TITLE = "JOBTITLE"
  val PLATFORM = "PLATFORM"
  val JOB_REFERENCE="JOBREFERENCE"
  val JOB_ADVERT = "JOBADVERT"
  val JOB_LOCATIONS = "JOBLOCATIONS"
  val START_DATE = "STARTDATE"
  val END_DATE = "ENDDATE"
  val IS_ACTIVE = "ISACTIVE"
  val PRIMARY_SKILLS_REQUIRED = "PRIMARYSKILLSREQUIRED"
  val SECONDARY_SKILLS_REQUIRED = "SECONDARYSKILLSREQUIRED"
  val JOB_APPLICATION = "JOBAPPLICATION"
}

object JobApplicationColumnConstants extends BaseColumnConstants {
  val JOB_ID = "JOBID"
  val CANDIDATE_ID = "CANDIDATEID"
  val USER_ID = "USERID"
  val CANDIDATE_SKILLS = "CANDIDATESKILLS"
  val ADMIN_SKILLS = "ADMINSKILLS"
  val APPROVED_SKILLS = "APPROVEDSKILLS"
  val QUALIFIED_RATIO = "QUALIFIEDRATIO"
  val JOB_STATUS = "JOBSTATUS"
  val IS_VERIFIED = "ISVERIFIED"

}

object Helper {
  def convertToObjectId(list: List[Int]): String = {
    val decimals = list.map { dec =>
      if (dec < 0) {
        dec + 256
      } else dec
    }
    val hexList = decimals.map(decimal => {
      val str = Integer.toHexString(decimal)
      if (str.length == 1) {
        "0" + str
      } else str
    })
    val objectId = hexList.mkString("")
    objectId
  }

  def checkFilePresentInFolder(folder: String, fileName: String): Boolean = {
    val folderPath = new File(folder)
    val files = folderPath.listFiles().toList
    val result = files.filter(x => x.isFile && x.getName == fileName)
    if (result.nonEmpty) true else false
  }
}