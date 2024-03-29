package com.rockthejvm

import play.api.libs.json.Json
import slick.jdbc.GetResult

import java.time.LocalDate
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
object PrivateExecutionContext {
  val executor = Executors.newFixedThreadPool(4)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(executor)
}
object Main {
  import CustomPostgresProfile.api._
  import PrivateExecutionContext._

  val shawshankRedemption = Movie(1L, "The Shawshank Redemption", LocalDate.of(1994, 9, 23), 162)
  val theMatrix = Movie(2L, "The Matrix", LocalDate.of(1993, 3, 31), 134)
  val phantomMenace = Movie(10L, "Star Wars: A Phantom Menace", LocalDate.of(1999, 5, 16), 133)
  val tomHanks = Actor(1L, "Tom Hanks")
  val juliaRoberts = Actor(2L, "Julia Roberts")
  val liamNesson = Actor(3L, "Liam Nesson")
  val providers = List(
    StreamingProviderMapping(0L, 1L, StreamingService.Netflix),
    StreamingProviderMapping(1L, 2L, StreamingService.Netflix),
    StreamingProviderMapping(2L, 10L, StreamingService.Disney),
  )
  val starWarsLocations = MovieLocations(1L, 4L, List("England", "Tunisia", "Italy"))
  val starWarsProperties = MovieProperties(1L, 4L, Map(
    "Genre" -> "Sci-Fi",
    "Features" -> "Lightsabers"
  ))
  val liamNessonDetails = ActorDetails(1L, 3L, Json.parse(
    """
      |{
      |  "born": 1952,
      |  "awesome": "yes"
      |}
      |""".stripMargin
  ))
  def demoInsertMovie(): Unit = {
    val queryDescription = SlickTables.movieTable += theMatrix
    val futureId: Future[Int] = Connection.db.run(queryDescription)
    futureId.onComplete {
      case Success(newMovieId) => println(s"Query was successful, new id is $newMovieId")
      case Failure(e) => println(s"Query failed, reason: $e")
    }
  }

  def demoInsertActors(): Unit = {
    val queryDescription = SlickTables.actorTable ++= Seq(tomHanks, juliaRoberts)
    val futureId = Connection.db.run(queryDescription)
    futureId.onComplete {
      case Success(_) => println(s"Query was successful")
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

  def readMoviesByPlainQuery(): Unit = {
    implicit val getResultMovie: GetResult[Movie] =
      GetResult(positionedResult => Movie(
        positionedResult.<<,
        positionedResult.<<,
        LocalDate.parse(positionedResult.nextString()),
        positionedResult.<<)
       )
    val query = sql"""select * from movies."Movie";""".as[Movie]
    Connection.db.run(query).onComplete {
      case Success(movies) => println(s"Query was successful, movies: $movies")
      case Failure(e) => println(s"Query failed, reason: $e")
    }
  }

  def multiQueriesSingleTransaction(): Unit = {
    val insertMovie = SlickTables.movieTable += phantomMenace
    val insertActor = SlickTables.actorTable += liamNesson
    val finalQuery = DBIO.seq(insertMovie, insertActor)
    Connection.db.run(finalQuery.transactionally).onComplete {
      case Success(_) => println(s"Query was successful")
      case Failure(e) => println(s"Query failed, reason: $e")
    }
  }

  def findAllActorsByMovie(movieId: Long): Future[Seq[Actor]] = {
    val joinQuery = SlickTables.movieActorTable
      .filter(_.movieId === movieId)
      .join(SlickTables.actorTable)
      .on(_.actorId === _.id)
      .map(_._2)
    Connection.db.run(joinQuery.result)
  }

  def addStreamingProviders() = {
    val insertQuery = SlickTables.streamingProviderMappingTable ++= providers
    Connection.db.run(insertQuery)
  }

  def findProvidersForMovie(movieId: Long): Future[Seq[StreamingProviderMapping]] = {
    val q = SlickTables.streamingProviderMappingTable.filter(_.movieId === movieId)
    Connection.db.run(q.result)
  }

  def insertLocations(): Unit = {
    val q = SpecialTables.movieLocationsTable += starWarsLocations
    Connection.db.run(q)
  }

  def insertProperties(): Unit =  {
    val q = SpecialTables.moviePropertiesTable += starWarsProperties
    Connection.db.run(q)
  }

  def insertActorDetails(): Unit = {
    val q = SpecialTables.actorPropertiesTable += liamNessonDetails
    Connection.db.run(q)
  }

  def findMoviesIn(location: String): Future[Seq[Movie]] = {
    val q = SpecialTables.movieLocationsTable
      .filter(_.locations @> List(location).bind)
      .join(SlickTables.movieTable)
      .on(_.movieId === _.id)
      .map(_._2)
    Connection.db.run(q.result)
  }

  def main(args: Array[String]): Unit = {
    findMoviesIn("Tunisia").onComplete {
      case Success(r) => println(s"Success: $r")
      case Failure(e) => println(s"Error: $e")
    }
    Thread.sleep(5000)
    PrivateExecutionContext.executor.shutdown()
  }
}
