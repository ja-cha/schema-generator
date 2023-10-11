package com.graphiti.sql

import slick.jdbc.JdbcProfile

/**
  * @author Jan Abt    
  * @date Nov 30, 2018
  */
trait DBComponent {

  val profile: JdbcProfile
  val db: profile.api.Database


}