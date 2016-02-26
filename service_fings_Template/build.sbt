name := "ContainerService"

version := "0.0." + sys.env.getOrElse("BUILD_NUMBER", "1")

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "http://buildprod2.lxc2.prod.amz.fullfacing.com:8081/nexus/content/repositories/snapshots",
  "Sonatype OSS Releases" at "http://buildprod2.lxc2.prod.amz.fullfacing.com:8081/nexus/content/repositories/releases"
)

libraryDependencies ++= {
  val scalazVersion = "7.2.0"
  val akkaVersion = "2.4.2"
  Seq(
    "ch.qos.logback" % "logback-core" % "1.1.5",
    "ch.qos.logback" % "logback-classic" % "1.1.5",
    "org.slf4j" % "slf4j-api" % "1.7.16",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "org.scalaz" %% "scalaz-core" % scalazVersion,
    "com.fullfacing" %% "common" % "0.15.229",
    "com.fullfacing" %% "common_fings" % "0.0.9"
  )
}
