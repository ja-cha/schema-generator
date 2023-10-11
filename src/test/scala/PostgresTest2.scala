/**
  * Created by jabt on 10/29/15.
  */

import org.scalatestplus.play.PlaySpec

import scala.collection.mutable.{LinkedHashMap, ListBuffer}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.{Failure, Success}

class PostgresTest2 extends PlaySpec {


  "init tables" in {
    Preview.init()
    Live.init()
  }


  "import data synchronously" in {

    Preview.init()
    Live.init()

    val failures = ListBuffer.empty[Int]
    val successes = ListBuffer.empty[Int]

    //<schema>.<tableName>
    val PATTERN = "(INSERT INTO )([a-z]+)(.*)".r

    val sqlQueries = Source.fromFile("src/main/resources/test-data-85-mb.sql").getLines //read file

    sqlQueries.foreach { query =>
      query match {

        case PATTERN(_, "live", _) =>
          val f = Live.run(Live.sqlu(query))

          Await.result(f, Duration.Inf)

          f.onComplete {
            case Failure(ex) =>
              println(s"error")
              println(s"reason : ${ex.getMessage}")
              failures += 1
            case Success(s) =>
              successes += 1
          }
        case PATTERN(_, "public", _) =>
          val f = Preview.run(Preview.sqlu(query))

          Await.result(f, Duration.Inf)

          f.onComplete {
            case Failure(ex) =>
              println(s"error")
              println(s"reason : ${ex.getMessage}")
              failures += 1

            case Success(s) =>
              successes += 1
          }

        case line => println(s"skipped $line")
      }
    }
    println(s"import finished with ${failures.sum} failures and ${successes.sum} successes.")
  }


  "import data asynchronously" in {

    Preview.init()
    Live.init()

    //<schema>.<tableName>
    val PATTERN = "(INSERT INTO )(.*)( VALUES)(.*)".r

    val sqlQueries = Source.fromFile("src/main/resources/test-data-85-mb.sql").getLines //read file

    var statementsBySchema = LinkedHashMap[String, List[String]]()
    val failures = ListBuffer.empty[String]
    val successes = ListBuffer.empty[Int]

    sqlQueries.foreach { query =>
      query match {
        case PATTERN(_, table, _, _) =>
          val lines = statementsBySchema.get(table).getOrElse(Nil)
          statementsBySchema += (table -> (query :: lines))
        case _ =>
      }
    }

    statementsBySchema.foreach {

      case (schema, statements) =>

        val futures = schema match {
          case table if (table.contains("live")) => {
            statements.map(statement => Live.run(Live.sqlu(statement)))
          }
          case table if (table.contains("public")) => {
            statements.map(statement => Preview.run(Preview.sqlu(statement)))
          }
        }

        val future = Future.sequence(futures)

        future.onComplete {
          case Failure(fail) =>
            failures += fail.getMessage
          case _ =>
            successes += 1
        }

        Await.ready(future, Duration.Inf)


    }
    println(s"import finished with ${failures.foreach(_ + "\n")} failures and  ${successes.sum} successes.")
  }

}