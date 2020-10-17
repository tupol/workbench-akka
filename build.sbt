name := "akka-cs"

version := "0.1"

scalaVersion := "2.13.3"

val akkaVersion     = "2.6.9"
val akkaHttpVersion = "10.1.11"
val akkaJson4s      = "1.30.0"
val json4s          = "3.6.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"            % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson"  % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j"                  % akkaVersion,
  "com.typesafe.akka" %% "akka-http"                   % akkaHttpVersion,
  "org.json4s"        %% "json4s-jackson"              % json4s,
  "org.json4s"        %% "json4s-ext"                  % json4s,
  "de.heikoseeberger" %% "akka-http-json4s"            % akkaJson4s,
  "ch.qos.logback"    % "logback-classic"              % "1.2.3",
  "com.typesafe.akka" %% "akka-actor-testkit-typed"    % akkaVersion % Test,
  "org.scalatest"     %% "scalatest"                   % "3.1.4" % Test
)
