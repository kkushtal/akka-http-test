lazy val akkaHttpVersion = "10.0.11"
lazy val akkaVersion = "2.5.11"
lazy val scalikejdbcVersion = "3.2.3"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.kushtal",
      scalaVersion    := "2.12.6"
    )),
    name := "akka-http-kushtal",
    libraryDependencies ++= Seq(
	  "org.postgresql"    % "postgresql"                 % "42.2.2",
	  "ch.qos.logback"    % "logback-classic"            % "1.2.3",
	  "org.scalikejdbc"   %% "scalikejdbc"               % scalikejdbcVersion,
	  "org.scalikejdbc"   %% "scalikejdbc-interpolation" % scalikejdbcVersion,
	  "org.scalikejdbc"   %% "scalikejdbc-config"        % scalikejdbcVersion,
	  
      "com.typesafe.akka" %% "akka-http"                 % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"      % akkaHttpVersion,
	  "com.typesafe.akka" %% "akka-http-caching"		 % akkaHttpVersion
    )
  )
