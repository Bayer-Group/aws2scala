import java.util.Date

// dependency versions
val akka = "2.4.2"
val aws = "1.10.52"

val compileOnlyOptions = Seq(
  "-deprecation",
  "-Xlint",
  "-Xverify",
  "-Yclosure-elim",
  "-Yinline"
)

lazy val commonSettings = Seq(
  // metadata
  homepage := Some(url("https://monsantoco.github.io/aws2scala")),
  organization := "com.monsanto.arch",
  organizationName := "Monsanto",
  organizationHomepage := Some(url("http://engineering.monsanto.com")),
  startYear := Some(2015),
  licenses := Seq("BSD New" → url("http://opensource.org/licenses/BSD-3-Clause")),

  // dependency resolution
  resolvers ++= Seq(
    "AWS" at "https://nexus.agro.services/content/repositories/releases/",
    "AWS Snapshots" at "https://nexus.agro.services/content/repositories/snapshots/",
    Resolver.jcenterRepo
  ),
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  dependencyOverrides ++= Set (
    "org.scala-lang"          % "scala-reflect" % scalaVersion.value,
    "org.scala-lang.modules" %% "scala-xml"     % "1.0.4",
    "org.slf4j"               % "slf4j-api"     % "1.7.16",
    "com.typesafe.akka"      %% "akka-actor"    % akka
  ),

  // scala compilation
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-unchecked",
    "-feature"
  ),
  (scalacOptions in Compile) ++= compileOnlyOptions,
  (scalacOptions in Test) := {
    val options = (scalacOptions in Test).value
    options.filterNot(compileOnlyOptions.contains)
  },

  // documentation
  apiMappingsScala ++= Map(
    ("com.typesafe.akka", "akka-actor") → "http://doc.akka.io/api/akka/%s",
    ("com.typesafe.akka", "akka-stream") → "http://doc.akka.io/api/akka/%s"
  ),
  apiMappingsJava ++= Map(
    ("com.typesafe", "config") → "http://typesafehub.github.io/config/latest/api"
  ) ++ createAwsApiMappings("core", "cloudformation", "ec2", "kms", "rds", "s3", "sns", "sts")
)

val AwsDocURL = "http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc"

def createAwsApiMappings(libs: String*) = libs.map(lib ⇒ ("com.amazonaws", s"aws-java-sdk-$lib") → AwsDocURL).toMap

lazy val bintrayPublishingSettings = Seq(
  bintrayOrganization := Some("monsanto"),
  bintrayPackageLabels := Seq("aws", "scala", "akka-streams"),
  bintrayVcsUrl := Some("https://github.com/MonsantoCo/aws2scala.git"),
  publishTo := {
    if (isSnapshot.value) Some("OJO Snapshots" at s"https://oss.jfrog.org/artifactory/oss-snapshot-local;build.timestamp=${new Date().getTime}")
    else publishTo.value
  },
  credentials ++= {
    List(bintrayCredentialsFile.value)
      .filter(_.exists())
      .map(f ⇒ Credentials.toDirect(Credentials(f)))
      .map(c ⇒ Credentials("Artifactory Realm", "oss.jfrog.org", c.userName, c.passwd))
  },
  bintrayReleaseOnPublish := {
    if (isSnapshot.value) false
    else bintrayReleaseOnPublish.value
  }
)

lazy val noPublishingSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

val commonDependencies = Seq(
  "com.typesafe.akka"           %% "akka-actor"                % akka,
  "com.typesafe.akka"           %% "akka-stream"               % akka,
  "com.typesafe.scala-logging"  %% "scala-logging"             % "3.1.0"
) ++ awsDependencies("core")

val commonTestDependencies = Seq(
  "com.typesafe.akka"  %% "akka-stream-testkit"               % akka        % "test",
  "com.typesafe.akka"  %% "akka-slf4j"                        % akka        % "test",
  "org.scalacheck"     %% "scalacheck"                        % "1.12.5"    % "test",
  "org.scalamock"      %% "scalamock-scalatest-support"       % "3.2.2"     % "test",
  "org.scalatest"      %% "scalatest"                         % "2.2.6"     % "test",
  "ch.qos.logback"      % "logback-classic"                   % "1.1.5"     % "test"
)

def awsDependencies(libs: String*): Seq[ModuleID] = libs.map(lib ⇒ "com.amazonaws" % s"aws-java-sdk-$lib" % aws)

lazy val `stream-support` = (project in file("stream-support"))
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "aws2scala-stream-support",
    description := "Macros and libraries to enable Akka stream support in aws2scala",
    libraryDependencies ++= Seq(
      "org.scala-lang"               % "scala-reflect"             % scalaVersion.value
    ) ++ commonTestDependencies ++ commonDependencies
  )

lazy val core = (project in file("core"))
  .dependsOn(`stream-support` % "compile-internal,test-internal,it-internal")
  .configs(IntegrationTest)
  .settings(commonSettings: _*)
  .settings(bintrayPublishingSettings: _*)
  .settings(Defaults.itSettings: _*)
  .settings(scalaUnidocSettings: _*)
  .settings(
    name := "aws2scala",
    description := "Utilities for consuming AWS APIs in Scala",
    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging"  %% "scala-logging"                       % "3.1.0",
      "io.spray"                    %% "spray-json"                          % "1.3.2" % "it,test",
      "com.monsanto.arch"           %% "cloud-formation-template-generator"  % "3.1.2" % "it,test",
      "commons-io"                   % "commons-io"                          % "2.4"   % "it,test"
    ) ++ commonTestDependencies.map(dep => dep.copy(configurations = Some("it,test")))
      ++ awsDependencies("cloudformation", "ec2", "iam", "kms", "rds", "s3", "sns", "sts").map(_ % "provided")
      ++ awsDependencies("sqs").map(_ % "it,test")
      ++ commonDependencies,
    mappings in (Compile, packageBin) ++= mappings.in(`stream-support`, Compile, packageBin).value,
    mappings in (Compile, packageSrc) ++= mappings.in(`stream-support`, Compile, packageSrc).value,
    mappings in (Compile, packageDoc) := mappings.in(ScalaUnidoc, packageDoc).value,
    (doc in ScalaUnidoc) <<= apiMappingsFixJavaLinks(doc in ScalaUnidoc)
  )

lazy val aws2scala = (project in file("."))
  .aggregate(`stream-support`, core)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
