scalaOrganization := "org.typelevel"

scalacOptions ++= Seq(
  "-Ypartial-unification", 
  "-Yliteral-types"
)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.typelevel" %% "cats-core" % "0.8.0"
)
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.2")
