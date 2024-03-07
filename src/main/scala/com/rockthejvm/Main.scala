package com.rockthejvm

import java.time.LocalDate
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
object PrivateExecutionContext {
  val executor = Executors.newFixedThreadPool(4)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(executor)
}
object Main {
  import slick.jdbc.PostgresProfile.api._
  import PrivateExecutionContext._

  val shawshankRedemption = Movie(1L, "The Shawshank Redemption", LocalDate.of(1994, 9, 23), 162)
  val theMatrix = Movie(2L, "The Matrix", LocalDate.of(1993, 3, 31), 134)
  def demoInsertMovie(): Unit = {
    val queryDescription = SlickTables.movieTable += theMatrix
    val futureId: Future[Int] = Connection.db.run(queryDescription)
    futureId.onComplete {
      case Success(newMovieId) => println(s"Query was successful, new id is $newMovieId")
      case Failure(e) => println(s"Query failed, reason: $e")
    }
  }

  def demoReadAllMovies(): Unit = {
    val resultFuture: Future[Seq[Movie]] = Connection.db.run(SlickTables.movieTable.result)
    resultFuture.onComplete {
      case Success(movies) => println(s"Fetched: ${movies.mkString(", ")}")
      case Failure(e) => println(s"Query failed, reason: $e")
    }
  }

  def demoReadSomeMovies(): Unit = {
    val resultFuture: Future[Seq[Movie]] = Connection.db.run(SlickTables.movieTable.filter(_.name.like("%Matrix%")).result)
    resultFuture.onComplete {
      case Success(movies) => println(s"Fetched: ${movies.mkString(", ")}")
      case Failure(e) => println(s"Query failed, reason: $e")
    }
  }

  def demoUpdate(): Unit = {
    val queryDescriptor = SlickTables.movieTable.filter(_.id === 1L).update(shawshankRedemption.copy(lengthInMin = 150))
    val futureId: Future[Int] = Connection.db.run(queryDescriptor)
    futureId.onComplete {
      case Success(newMovieId) => println(s"Query was successful, new id is $newMovieId")
      case Failure(e) => println(s"Query failed, reason: $e")
    }
  }

  def demoDelete(): Unit = {
    Connection.db.run(SlickTables.movieTable.filter(_.name.like("%Matrix%")).delete)
  }
  def main(args: Array[String]): Unit = {
    demoDelete()
    Thread.sleep(10000)
  }
}
