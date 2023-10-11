/**
* Created by jabt on 10/29/15.
*/

import com.graphiti.sql.public._
import org.scalatestplus.play.PlaySpec

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class PostgresPreviewSchemaTest extends PlaySpec  {
/**
  import profile.api._

  var failures=0
  var successes=0

  "drop EdgeRevTable" in {

    val q =EdgeRevTable.schema.drop

    Await.result(db.run(q), Duration.Inf)
    logger.info("test finished")
  }

  "drop EdgeTable" in {

    val q =EdgeTable.schema.drop

    Await.result(db.run(q), Duration.Inf)
    logger.info("test finished")
  }

  "drop NodeRevTable" in {

    val q =NodeRevTable.schema.drop

    Await.result(db.run(q), Duration.Inf)
    logger.info("test finished")
  }

  "drop NodeTable" in {

    val q =NodeTable.schema.drop

    Await.result(db.run(q), Duration.Inf)
    logger.info("test finished")
  }

  "drop CheckPointTable" in {

    val q =CheckpointTable.schema.drop

    Await.result(db.run(q), Duration.Inf)
    logger.info("test finished")
  }

  "create NodeTable" in {

    val q = NodeTable.schema.create

    Await.result(db.run(q), Duration.Inf)
    logger.info("test finished")
  }

  "create NodeRevTable" in {

    val q = NodeRevTable.schema.create

    Await.result(db.run(q), Duration.Inf)
    logger.info("test finished")
  }

  "create EdgeTable" in {

    val q = EdgeTable.schema.create

    Await.result(db.run(q), Duration.Inf)
    logger.info("test finished")
  }

 "create EdgeRevTable" in {

    val q = EdgeRevTable.schema.create

    Await.result(db.run(q), Duration.Inf)
    logger.info("test finished")
  }

  "create CheckPointTable" in {

    val q =CheckpointTable.schema.create

    Await.result(db.run(q), Duration.Inf)
    logger.info("test finished")
  }

  "insert from file test" in {

    val sqlQuery = Source.fromFile("src/main/resources/test-data-85-mb.sql").getLines //read file

    sqlQuery.foreach(line => {
        val sqlQuery =  sqlu"""#${line}"""
        db.run(sqlQuery.asTry).map{
          case Failure(ex) => {
            logger.error(s"error")
            logger.error(s"reason : ${ex.getMessage}")
            failures = failures + 1
          }
          case Success(s) =>
          //  logger.info(s"success")
          //  logger.info(s"$s")
            successes = successes + 1

        }
      }

    )
        val episodeCountQuery = sql"""select count(*) from node""".as[Int]
      val episodeCount = Await.result(db.run(episodeCountQuery), Duration.Inf)
      logger.info(s"counted $episodeCount")

    logger.debug("test finished, {} failures and {} successes", failures, successes)
  }

  "insert one test" in {

    val countQuery = sql"""select count(*) from live.node where id = '1wc524f86e3049476734o0e4f464d82f4f9c95ee51097'""".as[Int]

    val r= Await.result(db.run(countQuery), Duration.Inf)


    logger.debug(s"count $r")
  }

  "list Nodes" in {
    val q = NodeTable
    Await.result(db.run(q.result), Duration.Inf).map(println)
    logger.info("test finished")
  }

  "list Edges" in {
    val q = EdgeTable
    Await.result(db.run(q.result), Duration.Inf).map(println)
    logger.info("test finished")
  }


  "insert Node" in {

    val nt = Node("1", "2a428925c5ca4d50985fb8104681896d2")
    val q = NodeTable += nt
    val result =  Await.result( db.run(q), Duration.Inf )

    logger.info(s"${result}")
  }

 "insert another Node" in {

    val nt = Node("2", "2a428925c5ca4d50985fb8104681896d2")
    val q = NodeTable += nt
    val result =  Await.result( db.run(q), Duration.Inf )

    logger.info(s"${result}")
  }

  "delete single Node" in {

    val q = NodeTable
      .filter(nt => nt.id === "1")
      .delete
    val result =  Await.result( db.run(q), Duration.Inf )

    logger.info(s"deleted ${result} row")
  }
*/

}