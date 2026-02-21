ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "zio-9878-zscheduler-fix",
    libraryDependencies += "org.scalameta" %% "munit" % "1.1.0" % Test
  )
