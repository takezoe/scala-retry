name := "scala-retry"

organization := "com.github.takezoe"

version := "0.0.6"

scalaVersion := "2.13.4"

crossScalaVersions := Seq("2.12.12", "2.13.4", "2.11.12")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)

publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

scalacOptions := Seq("-deprecation", "-feature")

pomExtra := (
  <url>https://github.com/takezoe/scala-retry</url>
    <licenses>
      <license>
        <name>The Apache Software License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/takezoe/scala-retry</url>
      <connection>scm:git:https://github.com/takezoe/scala-retry.git</connection>
    </scm>
    <developers>
      <developer>
        <id>takezoe</id>
        <name>Naoki Takezoe</name>
      </developer>
    </developers>
  )