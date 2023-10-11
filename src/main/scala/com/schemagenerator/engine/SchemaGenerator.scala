package com.schemagenerator.engine


import org.slf4j.LoggerFactory
import slick.dbio._
import slick.jdbc._
import slick.jdbc.meta.MTable
import slick.model.Table
import scala.concurrent.ExecutionContext.Implicits.global


trait JdbcProfile {

  val profile:slick.jdbc.JdbcProfile
  val database: profile.backend.DatabaseFactory
  val slickDriverName:String
  val dbConfigPathPrefix:String

}

trait PostgresProfile  { this: JdbcProfile =>

  val profile    = slick.jdbc.PostgresProfile
  val database =  slick.jdbc.PostgresProfile.api.Database
  val slickDriverName = "slick.jdbc.PostgresProfile"
  val dbConfigPathPrefix = "postgres."

}
trait OracleProfile {this: JdbcProfile =>
  val profile    = slick.jdbc.OracleProfile
  val database =  slick.jdbc.OracleProfile.api.Database
  val slickDriverName = "slick.jdbc.OracleProfile"
  val dbConfigPathPrefix = "oracle."
}

trait MySQLProfile {this: JdbcProfile =>
  val profile =    slick.jdbc.MySQLProfile
  val database =   slick.jdbc.MySQLProfile.api.Database
  val slickDriverName = "slick.jdbc.MySQLProfile"
  val dbConfigPathPrefix = "mysql."
}



  class SchemaGenerator extends JdbcProfile with PostgresProfile{

  def etFilter =  (e:Entity, t:Table) => (e.table.toUpperCase == t.name.table.toUpperCase) & ( e.schema.toUpperCase == t.name.schema.map(_.toUpperCase).getOrElse(""))
  def emFilter = (e:Entity, mTable:MTable) => (e.table.toUpperCase == mTable.name.name.toUpperCase) & ( e.schema.toUpperCase == mTable.name.schema.map(_.toUpperCase).getOrElse(""))
  def tmFilter = (t:Table, mTable:MTable) => (t.name.table.toUpperCase == mTable.name.name.toUpperCase) & ( t.name.schema.map(_.toUpperCase).getOrElse("") == mTable.name.schema.map(_.toUpperCase).getOrElse(""))

  def logger = LoggerFactory.getLogger(this.getClass)

  def generateSchemaFiles(args: Array[String]): Unit = {

    val filename::packageName::tail=  args.toList

    tail.foreach(xmlfileName => {


      val xmlSchemas: List[SchemaMapping] = XmlBuilder(xmlfileName)

      logger.info( s"Mappings" )
      xmlSchemas.foreach{m =>
        logger.info( s"""\t ${m.schema}""")
        m.entities.foreach( e =>logger.info( s"""\t\t $e"""))
      }

      import scala.concurrent.Await
      import scala.concurrent.duration._

      for (xmlSchema <- xmlSchemas) {

        val dbConfigPath = dbConfigPathPrefix + xmlSchema.schema.toLowerCase //"slick.dbs.oracle.cp.dev"
        logger.info( s"")
        logger.info( s"proeccessing $xmlfileName with config settings: $dbConfigPath")

        val dbs = database.forConfig(dbConfigPath)

        try {

          val mTables: DBIO[Seq[MTable]] = (
              for {
              user <- SimpleJdbcAction(_.session.metaData.getUserName)
              mtables <- {
                logger.info( s"user $user")
                logger.info( s"schema ${xmlSchema.schema}")
                //arg @schemaPattern is case sensitive
                 MTable.getTables(None, Some(xmlSchema.schema), None, Some(Seq("TABLE")))
              }
            } yield
                mtables
            ).map{_.filter{mTable =>

            xmlSchema.entities.exists(emFilter(_, mTable))
            }
          }


          Await.result(

            dbs.run(profile.createModel(Some(mTables), ignoreInvalidDefaults = true))
              .map(model => {

              if(model.tables.size>0){

                model.tables.foreach(t => logger.info(s"$t"))

                val folder = new java.io.File(".").getCanonicalPath + "/src/main/scala/"
                SchemaMappingCodeGenerator(xmlSchema, filename, model).writeToFile(slickDriverName, folder, packageName+"."+xmlSchema.schema, filename, s"$filename.scala")

              }else{
                logger.info(s"no tables found")
              }


            })
            ,
            Duration.Inf)


        } finally dbs.close()
      }
      logger.info( s"""finished processing $xmlfileName""")

    })
  }
}

// The main application
object SchemaGenerator extends App{

     new SchemaGenerator().generateSchemaFiles(args)

}