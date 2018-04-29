package com.cvcloud.client.utils

import org.json4s.JsonAST.{JField, JString}
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
  * Created by DDM on 20-04-2017.
  */

//Can be used for conversion back and forth from json
object JsonImplicits {

  def toJson(value: Any): String = {
    if (value.isInstanceOf[String]) value.asInstanceOf[String] else convertToJValue(value)
  }

  implicit val format = DefaultFormats

  def convertToJValue(x: Any) = compact(Extraction.decompose(x))

  def extractEntity[A](json: String)(implicit formats: Formats, mf: Manifest[A]): A = parse(json).extract[A]

  def updateObject[A](obj: A, key: String, value: String)(implicit formats: Formats, mf: Manifest[A]): A = {
    val json = toJson(obj)
    val result = parse(json).transformField { case JField(x, v) if x == key => JField(key, JString(value)) }
    extractEntity[A](toJson(result))
  }
}