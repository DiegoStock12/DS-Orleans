name := "DS-Orleans"
version := "0.1"
scalaVersion := "2.12.10"
// Logging dependencies
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

libraryDependencies ++= Seq(
  // ScalaPB dependencies
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "org.json4s" %% "json4s-jackson" % "3.7.0-M2",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0"
  //  "io.higherkindness" %% "mu-rpc-server" % "0.21.3",
  //  "io.higherkindness" %% "mu-rpc-channel" % "0.21.3",
  //  "com.typesafe" % "config" % "1.3.2",
  //  "org.typelevel" %% "cats-core" % "2.1.1",
  //  "org.typelevel" %% "cats-effect" % "2.1.2"
)

// Necessary for compiling protobuf files
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)
