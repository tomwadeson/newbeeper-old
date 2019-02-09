ThisBuild / resolvers += Resolver.sonatypeRepo("releases")
ThisBuild / libraryDependencies += compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
ThisBuild / libraryDependencies += compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0-M4")

enablePlugins(JavaAppPackaging)

ThisBuild / organization := "com.newbeeper"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.8"

val fs2Version                 = "1.0.2"
val catsVersion                = "1.5.0"
val catsEffectVersion          = "1.1.0"
val http4sVersion              = "0.20.0-M5"
val circeVersion               = "0.11.0"
val pureconfigVersion          = "0.10.1"
val shapelessVersion           = "2.3.3"
val scalatestVersion           = "3.0.5"
val scalacheckVersion          = "1.14.0"
val scalacheckShapelessVersion = "1.2.0-1"

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest"              %% "scalatest"                 % scalatestVersion           % Test,
    "org.scalacheck"             %% "scalacheck"                % scalacheckVersion          % Test,
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % scalacheckShapelessVersion % Test
  )
)

lazy val root =
  (project in file("."))
    .settings(
      name := "newbeeper",
      mainClass in Compile := Some("newbeeper.api.Main")
    )
    .aggregate(core, api)
    .dependsOn(core, api)

lazy val core =
  (project in file("core"))
    .settings(commonSettings)
    .settings(
      name := "newbeeper-core",

      libraryDependencies ++= Seq(
        "co.fs2"        %% "fs2-core"    % fs2Version,
        "co.fs2"        %% "fs2-io"      % fs2Version,
        "org.typelevel" %% "cats-core"   % catsVersion,
        "org.typelevel" %% "cats-effect" % catsEffectVersion,
        "com.chuusai"   %% "shapeless"   % shapelessVersion
      )
    )

lazy val api =
  (project in file("api"))
    .settings(commonSettings)
    .settings(
      name := "newbeeper-api",

      libraryDependencies ++= Seq(
        "org.http4s"            %% "http4s-core"          % http4sVersion,
        "org.http4s"            %% "http4s-dsl"           % http4sVersion,
        "org.http4s"            %% "http4s-blaze-server"  % http4sVersion,
        "org.http4s"            %% "http4s-blaze-client"  % http4sVersion,
        "org.http4s"            %% "http4s-circe"         % http4sVersion,
        "io.circe"              %% "circe-core"           % circeVersion,
        "io.circe"              %% "circe-generic-extras" % circeVersion,
        "com.github.pureconfig" %% "pureconfig"           % pureconfigVersion
      )
    ).dependsOn(core)
