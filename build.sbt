name := "GatlingProjectOne"

version := "0.1"

scalaVersion := "2.12.19"

libraryDependencies ++= Seq(
  "io.gatling" % "gatling-test-framework" % "3.9.0" % Test,
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.9.0"
)

resolvers += Resolver.sonatypeRepo("public")

