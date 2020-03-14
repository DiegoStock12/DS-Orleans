name := "DS-Orleans"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  // ScalaPB dependencies
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
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