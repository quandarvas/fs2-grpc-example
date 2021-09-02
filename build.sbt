name := "grpc-test"

version := "0.1"

scalaVersion := "3.0.1"

enablePlugins(Fs2Grpc)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.2.2",
  "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
  "org.slf4j" % "slf4j-api" % "1.7.32",
  "ch.qos.logback" % "logback-classic" % "1.2.5"
)
