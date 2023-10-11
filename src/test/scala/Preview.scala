import com.graphiti.sql.DBComponent
import com.graphiti.sql.public.{PostgresTables => PreviewTables}
import org.slf4j.LoggerFactory
import slick.jdbc.meta.MTable
import slick.sql.SqlAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Created by jabt on 2/3/16.
  */


object Preview extends PreviewTables with DBComponent {

  val profile = slick.jdbc.PostgresProfile

  val db = profile.api.Database.forConfig("slick.dbs.postgres.cp.local")

  val logger = LoggerFactory.getLogger(this.getClass)

  import profile.api._

  def sqlu(param: String): SqlAction[Int, NoStream, Effect] = sqlu"""#${param}"""

  def run[R](a: slick.dbio.DBIOAction[R, NoStream, Nothing]): Future[R] = db.run(a);

  def init() = {


    val catalog = Await.result(db.run(MTable.getTables), 1.second).filter(mt => {
      val tableName = s"${mt.name.schema.get}.${mt.name.name}"
      List("public.checkpoint", "public.node_rev", "public.node", "public.edge_rev", "public.edge").contains(tableName)
    })

    if (catalog.size > 0) {
      catalog.foreach { table =>
        table.name.name match {
          case "checkpoint" => {
            db.run(DBIO.seq(CheckpointTable.schema.drop, CheckpointTable.schema.create).asTry).map {
              case Failure(ex) => {
                println(s"error, reason: ${ex.getMessage}")
              }
              case Success(s) =>
            }
          }
          case "node_rev" => {
            db.run(DBIO.seq(NodeRevTable.schema.drop, NodeRevTable.schema.create).asTry).map {
              case Failure(ex) => {
                println(s"error, reason: ${ex.getMessage}")
              }
              case Success(s) =>
            }
          }
          case "node" => {
            db.run(DBIO.seq(NodeTable.schema.drop, NodeTable.schema.create).asTry).map {
              case Failure(ex) => {
                println(s"error, reason: ${ex.getMessage}")
              }
              case Success(s) =>
            }
          }
          case _ => println(s"no match for $table")
          case "edge_rev" => {
            db.run(DBIO.seq(EdgeRevTable.schema.drop, EdgeRevTable.schema.create).asTry).map {
              case Failure(ex) => {
                println(s"error, reason: ${ex.getMessage}")
              }
              case Success(s) =>
            }
          }
          case "edge" => {
            db.run(DBIO.seq(EdgeTable.schema.drop, EdgeTable.schema.create).asTry).map {
              case Failure(ex) => {
                println(s"error, reason: ${ex.getMessage}")
              }
              case Success(s) =>
            }
          }
          case _ => println(s"no match for $table")
        }
      }
    } else {

      db.run(NodeRevTable.schema.create.asTry).map {
        case Failure(ex) => {
          println(s"error, reason: ${ex.getMessage}")
        }
        case Success(s) =>
      }

      db.run(NodeTable.schema.create.asTry).map {
        case Failure(ex) => {
          println(s"error, reason: ${ex.getMessage}")
        }
        case Success(s) =>
      }
      db.run(EdgeRevTable.schema.create.asTry).map {
        case Failure(ex) => {
          println(s"error, reason: ${ex.getMessage}")
        }
        case Success(s) =>
      }

      db.run(EdgeTable.schema.create.asTry).map {
        case Failure(ex) => {
          println(s"error, reason: ${ex.getMessage}")
        }
        case Success(s) =>
      }
    }

  }

}


