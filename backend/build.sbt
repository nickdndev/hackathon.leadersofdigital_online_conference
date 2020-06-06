lazy val commonSettings = Seq(
    name := "rooms",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.12.8",
    scalacOptions ++= Seq(
        "-deprecation",
        "-Xfatal-warnings",
        "-Ywarn-value-discard",
        "-Xlint:missing-interpolator",
        "-Ypartial-unification"
    ),
)

lazy val Http4sVersion = "0.21.1"

lazy val DoobieVersion = "0.8.8"

lazy val H2Version = "1.4.200"

lazy val FlywayVersion = "6.3.1"

lazy val CirceVersion = "0.13.0"

lazy val PureConfigVersion = "0.12.3"

lazy val LogbackVersion = "1.2.3"

lazy val ScalaTestVersion = "3.1.1"

lazy val ScalaMockVersion = "4.4.0"

lazy val root = (project in file("."))
    .configs(IntegrationTest)
    .enablePlugins(JavaAppPackaging)
    .settings(
        commonSettings,
        Defaults.itSettings,
        libraryDependencies ++= Seq(
            "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
            "org.http4s" %% "http4s-circe" % Http4sVersion,
            "org.http4s" %% "http4s-dsl" % Http4sVersion,
            "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
            "org.tpolecat" %% "doobie-core" % DoobieVersion,
            "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
            "org.tpolecat" %% "doobie-postgres"  % DoobieVersion,
         //   "com.h2database" % "h2" % H2Version,
            "org.redisson" % "redisson" % "3.7.0" exclude("io.netty", "*"),
            "io.netty" % "netty-resolver-dns" % "4.1.17.Final",

            "org.flywaydb" % "flyway-core" % FlywayVersion,

            "io.circe" %% "circe-generic" % CirceVersion,
            "io.circe" %% "circe-literal" % CirceVersion % "it,test",
            "io.circe" %% "circe-optics" % CirceVersion % "it",

            "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
            "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion,

            "ch.qos.logback" % "logback-classic" % LogbackVersion,

            "org.scalatest" %% "scalatest" % ScalaTestVersion % "it,test",
            "org.scalamock" %% "scalamock" % ScalaMockVersion % "test"
        )
    )