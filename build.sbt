name := "DS-Orleans"
version := "0.1"
scalaVersion := "2.12.10"
// Logging dependencies
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

libraryDependencies ++= Seq(
  // ScalaPB dependencies
  "org.json4s" %% "json4s-jackson" % "3.7.0-M2",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0",
//  "io.higherkindness" %% "mu-rpc-server" % "0.21.3",
//  "io.higherkindness" %% "mu-rpc-channel" % "0.21.3",
//  "com.typesafe" % "config" % "1.3.2",
//  "org.typelevel" %% "cats-core" % "2.1.1",
//  "org.typelevel" %% "cats-effect" % "2.1.2"
)


lazy val compilerOptions = Seq(
  //  "-unchecked",
  //  "-feature",
  //  "-language:existentials",
  //  "-language:higherKinds",
  //  "-language:implicitConversions",
  //  "-language:postfixOps",
  //  "-deprecation",
  "-encoding",
  "utf8"
)

lazy val assemblySettings = Seq(
  assemblyJarName in assembly := name.value + ".jar",
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*)  => MergeStrategy.discard
    case "log4j.properties"             => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

assembly / mainClass := Some("org.orleans.Main")

// make run command include the provided dependencies
Compile / run  := Defaults.runTask(Compile / fullClasspath,
  Compile / run / mainClass,
  Compile / run / runner
).evaluated

Compile / run / fork := true
Global / cancelable := true

// exclude Scala library from assembly
assembly / assemblyOption  := (assembly / assemblyOption).value.copy(includeScala = false)
