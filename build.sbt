organization := "com.github.acflorea"
name := "akka-persistence-titan"
version := "1.0.1-SNAPSHOT"

scalaVersion := "2.11.7"

val akkaVersion = "2.4.1"
val logbackVersion = "1.1.3"
val scalatestVersion = "2.2.4"
val titanVersion = "1.0.0"
val configVersion = "1.3.0"
val gsonVersion = "2.7"
val liftVersion = "2.0"

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:postfixOps",
  "-target:jvm-1.8")

parallelExecution in ThisBuild := false

parallelExecution in Test := false
logBuffered in Test := false

libraryDependencies ++= {
  Seq(
    "ch.qos.logback" % "logback-classic" % logbackVersion,

    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % "test",

    // GSON
    "com.google.code.gson" % "gson" % gsonVersion,

  // Titan
    "com.thinkaurelius.titan" % "titan-core" % titanVersion,
    "com.thinkaurelius.titan" % "titan-cassandra" % titanVersion % "test",
    "com.thinkaurelius.titan" % "titan-es" % titanVersion % "test",
    "com.typesafe.akka" %% "akka-persistence-tck" % akkaVersion % "test"
  )
}

// 1.1.2 seems to no longer be available on ivy
dependencyOverrides += "com.github.stephenc.high-scale-lib" % "high-scale-lib" % "1.1.4"


publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := {
  <url>https://github.com/acflorea/akka-persistence-titan</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/acflorea/akka-persistence-titan.git</connection>
      <developerConnection>scm:git:git@github.com:acflorea/akka-persistence-titan.git</developerConnection>
      <url>github.com/acflorea/akka-persistence-titan.git</url>
    </scm>
    <developers>
      <developer>
        <id>acflorea</id>
        <name>Adrian Florea</name>
        <url>https://github.com/acflorea/</url>
      </developer>
    </developers>
}
