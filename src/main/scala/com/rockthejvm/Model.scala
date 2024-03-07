package com.rockthejvm

import play.api.libs.json.JsValue
import slick.lifted.ProvenShape

import java.time.LocalDate

case class Movie(id: Long, name: String, releaseDate: LocalDate, lengthInMin: Int)
case class Actor(id: Long, name: String)
case class MovieActorMapping(id: Long, movieId: Long, actorId: Long)
case class StreamingProviderMapping(id: Long, movieId: Long, streamingProvider: StreamingService.Provider)
// part 4
case class MovieLocations(id: Long, movieId: Long, locations: List[String])
case class MovieProperties(id: Long, movieId: Long, properties: Map[String, String])
case class ActorDetails(id: Long, actorId: Long, personalDetails: JsValue)
object StreamingService extends Enumeration {
  type Provider = Value
  val Netflix = Value("Netflix")
  val Disney = Value("Disney")
  val Prime = Value("Prime")
  val Hulu = Value("Hulu")
}
object SlickTables {
  import slick.jdbc.PostgresProfile.api._

  class MovieTable(tag: Tag) extends Table[Movie](tag, Some("movies"), "Movie") {
    def id = column[Long]("movie_id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def releaseDate = column[LocalDate]("release_date")
    def lengthInMin = column[Int]("length_in_min")
    override def * : ProvenShape[Movie] = (id, name, releaseDate, lengthInMin) <> (Movie.tupled, Movie.unapply)
  }

  lazy val movieTable = TableQuery[MovieTable]

  class ActorTable(tag: Tag) extends Table[Actor](tag, Some("movies"), "Actor") {
    def id = column[Long]("actor_id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    override def * : ProvenShape[Actor] = (id, name) <> (Actor.tupled, Actor.unapply)
  }

  lazy val actorTable = TableQuery[ActorTable]

  class MovieActorMappingTable(tag: Tag) extends Table[MovieActorMapping](tag, Some("movies"), "MovieActorMapping") {
    def id = column[Long]("movie_actor_id", O.PrimaryKey, O.AutoInc)
    def movieId = column[Long]("movie_id")
    def actorId = column[Long]("actor_id")
    override def * : ProvenShape[MovieActorMapping] = (id, movieId, actorId) <> (MovieActorMapping.tupled, MovieActorMapping.unapply)
  }

  lazy val movieActorTable = TableQuery[MovieActorMappingTable]

  class StreamingProviderMappingTable(tag: Tag) extends Table[StreamingProviderMapping](tag, Some("movies"), "StreamingProviderMapping") {
    implicit val providerMapper = MappedColumnType.base[StreamingService.Provider, String](
      provider => provider.toString,
      string => StreamingService.withName(string)
    )
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def movieId = column[Long]("movie_id")
    def streaming_provider = column[StreamingService.Provider]("streaming_provider")
    override def * : ProvenShape[StreamingProviderMapping] = (id, movieId, streaming_provider) <> (StreamingProviderMapping.tupled, StreamingProviderMapping.unapply)
  }

  lazy val streamingProviderMappingTable = TableQuery[StreamingProviderMappingTable]

  val tables = List(movieTable, actorTable, movieActorTable, streamingProviderMappingTable)
  val ddl = tables.map(_.schema).reduce(_ ++ _)
}

object SpecialTables {
  import CustomPostgresProfile.api._

  class MovieLocationsTable(tag: Tag) extends Table[MovieLocations](tag, Some("movies"), "MovieLocations") {
    def id = column[Long]("movie_location_id", O.PrimaryKey, O.AutoInc)
    def movieId = column[Long]("movie_id")
    def locations = column[List[String]]("locations")
    override def * : ProvenShape[MovieLocations] = (id, movieId, locations) <> (MovieLocations.tupled, MovieLocations.unapply)
  }

  lazy val movieLocationsTable = TableQuery[MovieLocationsTable]

  class MoviePropertiesTable(tag: Tag) extends Table[MovieProperties](tag, Some("movies"), "MovieProperties") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def movieId = column[Long]("movie_id")
    def properties = column[Map[String, String]]("properties")
    override def * : ProvenShape[MovieProperties] = (id, movieId, properties) <> (MovieProperties.tupled, MovieProperties.unapply)
  }

  lazy val moviePropertiesTable = TableQuery[MoviePropertiesTable]

  class ActorDetailsTable(tag: Tag) extends Table[ActorDetails](tag, Some("movies"), "ActorDetails") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def actorId = column[Long]("actor_id")
    def personalDetails = column[JsValue]("personal_info")
    override def * : ProvenShape[ActorDetails] = (id, actorId, personalDetails) <> (ActorDetails.tupled, ActorDetails.unapply)
  }

  lazy val actorPropertiesTable = TableQuery[ActorDetailsTable]
}
object TableDefinitionGenerator extends App {
  println(SlickTables.ddl.createIfNotExistsStatements.mkString(";\n"))
}
