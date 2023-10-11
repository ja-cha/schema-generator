package com.schemagenerator.engine

import java.sql.Timestamp
import java.text.SimpleDateFormat

import org.slf4j.LoggerFactory


trait Globals   {

  def logger = LoggerFactory.getLogger(this.getClass)

  /**
    * current Timestamp
    * @return Timestamp
    */
  def now = new Timestamp(System.currentTimeMillis())
  /**
    * shorthand for a Timestamp instance
    * accepting a String in any of the following formats:
    * yyyy-MM-dd
    * yyyy-MM-dd hh:mm:ss
    * yyyy-MM-dd hh:mm:ss.SSS
    */
  val ts = new PartialFunction[String, Timestamp] {
    def apply(dateString: String) = {
      val df = new SimpleDateFormat(patternFor(dateString).get)
      new Timestamp(df.parse(dateString).getTime)
    }
    def isDefinedAt(dateString: String) = patternFor(dateString).isDefined

    def patternFor(ts: String):Option[String]= {
      val regExGroup = s"(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)(\\s\\d\\d:\\d\\d:\\d\\d)?(.\\d{1,3})?".r
      ts match {
        case regExGroup(yyyy, mm, dd, hms, ms) if( hms == null & ms == null) => Some(s"yyyy-MM-dd")
        case regExGroup(yyyy, mm, dd, hms, ms) if( ms ==null) => Some(s"yyyy-MM-dd HH:mm:ss")
        case regExGroup(yyyy, mm, dd, hms, ms) if( hms != null & ms != null)=> Some(s"yyyy-MM-dd HH:mm:ss.SSS")
        case _ => None
      }
    }
  }
  /**
    * shorthand for a Timestamp instance
    * accepting a number of the following format:
    * yyyymmdd
    */
  val dt = new PartialFunction[AnyVal, Timestamp] {

    def apply(dateNumber: AnyVal) = {
      val dateString = format(dateNumber)
      val df = new SimpleDateFormat("yyyy/MM/dd")
      new Timestamp(df.parse(dateString.get).getTime)
    }
    def isDefinedAt(dateNumber: AnyVal) = !format(dateNumber).isEmpty
    def format(ts: AnyVal):Option[String]= {
      val unformatted = ts.toString
      val regExGroup = s"([0-9][0-9][0-9][0-9])([0-9][0-9])([0-9][0-9])".r
      unformatted match {
        case regExGroup(y, m, d) => Some(s"$y/$m/$d")
        case _ => None
      }
    }

  }
  /**
  * get the first one thsat is defined
  */
  val applyEither = new PartialFunction[Option[_] , Option[_] ]{
    def apply(f: Option[_] ):Option[_] = {f}
    def isDefinedAt(s: Option[_]) = {!s.isEmpty}
  }
}
