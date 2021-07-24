name := "sendme-backend-challenge"

version := "0.1"

scalaVersion := "2.13.6"

val AkkaVersion          = "2.6.15"
val AkkaHttpVersion      = "10.2.4"
val AkkaHttpCirceVersion = "1.36.0"
val BcryptVersion        = "4.1"
val CirceVersion         = "0.14.1"
val LogbackVersion       = "1.2.3"
val PostgresVersion      = "42.2.18"
val ScalaTestVersion     = "3.2.9"
val SlickVersion         = "3.3.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-stream-typed"   % AkkaVersion,
  "com.typesafe.akka"  %% "akka-http"           % AkkaHttpVersion,
  "com.github.t3hnar"  %% "scala-bcrypt"        % BcryptVersion,
  // JSON parsing
  "de.heikoseeberger"  %% "akka-http-circe"     % AkkaHttpCirceVersion,
  "io.circe"           %% "circe-core"          % CirceVersion,
  "io.circe"           %% "circe-generic"       % CirceVersion,
  "io.circe"           %% "circe-parser"        % CirceVersion,
  // Databsae
  "com.typesafe.slick" %% "slick"               % SlickVersion,
  "org.postgresql"      % "postgresql"          % PostgresVersion,
  // Logging
  "ch.qos.logback"      % "logback-classic"     % LogbackVersion,
  // Test
  "com.typesafe.akka"  %% "akka-stream-testkit" % AkkaVersion      % Test,
  "com.typesafe.akka"  %% "akka-http-testkit"   % AkkaHttpVersion  % Test,
  "org.scalatest"      %% "scalatest"           % ScalaTestVersion % Test
)
