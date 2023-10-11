package com.schemagenerator

/**
  * Created by jabt on 9/21/15.
  */
package object engine {

  implicit class StringExtensions(val str: String) {
    final def unCapitalize: String = str(0).toString.toLowerCase + str.tail
    final def toSimpleClassName: String = str.split('.').last.capitalize
    final def toCamelCase: String = str.toLowerCase
      .split("_")
      .map { case "" => "_" case s => s }
      .map(_.capitalize)
      .mkString("")
  }

}
