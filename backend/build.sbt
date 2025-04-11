import com.typesafe.sbt.packager.docker.DockerApiVersion

// ############### VARIABLES ###############
val ProjectName: String = "backend"
val ProjectVersion: String = "1.0.0"
val ScalaVersion: String = "3.3.5"

// ############### SETTINGS ###############
val dockerSettings = Seq(
  Docker / packageName := ProjectName,
  Docker / version := ProjectVersion,
  Docker / maintainer := "marco.paggioro@studenti.unipegaso.it",
  dockerBaseImage := "eclipse-temurin:23",
  dockerExposedPorts := Seq(9000),
  dockerApiVersion := Some(DockerApiVersion(1, 48))
)

val allowedWarts = Warts.unsafe.filterNot(
  Seq(
    Wart.NonUnitStatements,
    Wart.StringPlusAny,
    Wart.Throw,
    Wart.DefaultArguments
  ).contains(_)
)


lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin, DockerPlugin, JavaAppPackaging)
  .settings(
    name := ProjectName,
    version := ProjectVersion,
    scalaVersion := ScalaVersion,
    run / fork := true,
    dockerSettings,
    // wartremover
    Compile / compile / wartremoverErrors ++= allowedWarts,
    wartremoverExcluded += baseDirectory.value / "target"
  )

// ############### DEPENDENCIES ###############
resolvers += "Akka library repository".at("https://repo.akka.io/maven")

val AkkaVersion = "2.10.2"
val AkkaPersistenceJdbcVersion = "5.5.1"
val AkkaProjectionVersion = "1.6.10"
val AkkaHttpVersion = "10.7.0"
val FlywayVersion: String = "11.6.0"
val PostgresqlVersion: String = "42.7.5"
val SlickVersion = "3.5.2"
val SlickPgVersion = "0.22.2"
val CirceVersion: String = "0.14.12"
val JwtCirceVersion: String = "10.0.4"
val LogbackVersion: String = "1.5.18"
val ScalaLoggingVersion: String = "3.9.5"
val PprintVersion: String = "0.9.0"
val CatsVersion: String = "2.13.0"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
  "com.lightbend.akka" %% "akka-persistence-jdbc" % AkkaPersistenceJdbcVersion,
  "com.lightbend.akka" %% "akka-projection-eventsourced" % AkkaProjectionVersion,
  "com.lightbend.akka" %% "akka-projection-jdbc" % AkkaProjectionVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,

  // Database
  "org.flywaydb" % "flyway-core" % FlywayVersion,
  "org.flywaydb" % "flyway-database-postgresql" % FlywayVersion % Runtime,
  "org.postgresql" % "postgresql" % PostgresqlVersion % Runtime,
  "com.typesafe.slick" %% "slick" % SlickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
  "com.github.tminglei" %% "slick-pg" % SlickPgVersion,

  // JSON
  "io.circe" %% "circe-core" % CirceVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-parser" % CirceVersion,
  "com.github.jwt-scala" %% "jwt-circe" % JwtCirceVersion,

  // Logs
  "ch.qos.logback" % "logback-classic" % LogbackVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
  "com.lihaoyi" %% "pprint" % PprintVersion,

  // Others
  "org.typelevel" %% "cats-core" % CatsVersion
)