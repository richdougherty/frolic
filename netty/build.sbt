name := "frolic-netty"

organization := "com.richdougherty.frolic"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  organization.value %% "frolic-core" % version.value,
  "io.netty" % "netty-all" % "4.0.19.Final"
)
