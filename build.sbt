name := "esklep"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.25",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.25" % "test",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.25",
  "org.iq80.leveldb" % "leveldb" % "0.9",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test")