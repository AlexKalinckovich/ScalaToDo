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
val MunitVersion = "1.2.1"
val MunitCatsEffectVersion = "2.1.0"
val MockitoScalaVersion = "2.0.0"
val ScalaTestVersion = "3.2.19"
val ScalaTestPlusVersion = "3.2.19.0"

val TestContainersVersion = "1.9.1"
val TestContainersScalaVersion = "0.43.6"

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
    "org.scalameta" %% "munit" % MunitVersion % Test,
    "org.typelevel" %% "munit-cats-effect" % MunitCatsEffectVersion % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
    "org.scalatestplus" %% "mockito-5-12" % ScalaTestPlusVersion % Test,

    "com.dimafeng" %% "testcontainers-scala-munit" % TestContainersScalaVersion % Test,
    "com.dimafeng" %% "testcontainers-scala-postgresql" % TestContainersScalaVersion % Test,

    "org.testcontainers" % "testcontainers" % TestContainersVersion % Test,
    "org.testcontainers" % "postgresql" % TestContainersVersion % Test,
    "org.testcontainers" % "jdbc" % TestContainersVersion % Test

)

testFrameworks += new TestFramework("munit.Framework")
testFrameworks += new TestFramework("org.scalatest.tools.Framework")
