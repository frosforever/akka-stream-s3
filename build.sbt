name := "s3 streaming"

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-language:higherKinds",
  "-language:implicitConversions",
//  "-Xfatal-warnings",
  "-Xlint",
  "-Yinline-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Ywarn-unused-import" // This might not work well. Try it out for now
)

javacOptions ++= Seq(
  "-source", "1.8",
  "-target", "1.8",
  "-Xlint"
)

val AkkaHttp = "2.4.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaHttp,
  "com.typesafe.akka" %% "akka-http-experimental" % AkkaHttp,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % AkkaHttp,
  "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttp % "test",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.33"
)

Revolver.settings
