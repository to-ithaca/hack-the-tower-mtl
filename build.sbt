scalaOrganization := "org.typelevel"

scalacOptions ++= Seq(
  "-Ypartial-unification", 
  "-Yliteral-types"
)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)
