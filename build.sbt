ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.3"

lazy val root = (project in file("."))
  .settings(
    name := "ScalaToDo"
  )

val Http4sVersion = "0.23.33"
val CirisVersion = "3.11.1"
val LiquibaseVersion = "5.0.1"
val PostgresVersion = "42.7.8"
val LogbackVersion = "1.5.20"
val CirceVersion = "0.14.15"
val DoobieVersion = "1.0.0-RC10"

libraryDependencies ++= Seq(
    "org.http4s" %% "http4s-ember-server" % Http4sVersion,
    "org.http4s" %% "http4s-ember-client" % Http4sVersion,
    "org.http4s" %% "http4s-dsl" % Http4sVersion,
    "org.http4s" %% "http4s-circe" % Http4sVersion,
    "is.cir" %% "ciris" % CirisVersion,
    "is.cir" %% "ciris-http4s" % CirisVersion,
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-literal" % CirceVersion,
    "org.liquibase" % "liquibase-core" % LiquibaseVersion,
    "org.postgresql" % "postgresql" % PostgresVersion,
    "org.tpolecat" %% "doobie-core" % DoobieVersion,
    "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
    "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
)
