import java.util.Date
import UnidocKeys._

// dependency versions
val akka = "2.4.4"
val aws = "1.10.75"
val scalaCheck = "org.scalacheck"     %% "scalacheck"                          % "1.12.5"
val scalaTest  = "org.scalatest"      %% "scalatest"                           % "2.2.6"
val sprayJson  = "io.spray"           %% "spray-json"                          % "1.3.2"
val cftg       = "com.monsanto.arch"  %% "cloud-formation-template-generator"  % "3.3.3"

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

  // scala compilation
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-unchecked",
    "-feature"
  ),
  (scalacOptions in Compile) ++= compileOnlyOptions,
  (scalacOptions in Test) --= compileOnlyOptions,

  // Needed to avoid OOM errors
  (fork in Test) := true,

  // documentation
  apiMappingsScala ++= Map(
    ("com.typesafe.akka", "akka-actor") → "http://doc.akka.io/api/akka/%s",
    ("com.typesafe.akka", "akka-stream") → "http://doc.akka.io/api/akka/%s"
  ),
  apiMappingsJava ++= Map(
    ("com.typesafe", "config") → "http://typesafehub.github.io/config/latest/api"
  ) ++ createAwsApiMappings("core", "cloudformation", "ec2", "kms", "rds", "s3", "sns", "sts"),

  // coverage
  coverageExcludedPackages := "com\\.monsanto\\.arch\\.awsutil\\.test_support\\..*;com\\.monsanto\\.arch\\.awsutil\\.testkit\\..*",

  // Allow resolution on JCenter
  resolvers += Resolver.jcenterRepo
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
  "com.typesafe.akka"           %% "akka-stream"    % akka,
  "com.typesafe.scala-logging"  %% "scala-logging"  % "3.4.0",
  awsDependency("core")
)

def awsDependency(lib: String): ModuleID = "com.amazonaws" % s"aws-java-sdk-$lib" % aws

def awsDependencies(libs: String*): Seq[ModuleID] = libs.map(awsDependency)

lazy val testSupport = Project("aws2scala-test-support", file("aws2scala-test-support"))
  .settings(
    commonSettings,
    noPublishingSettings,
    description := "Common configuration and utilities for testing in aws2scala",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"  %% "akka-slf4j"                   % akka,
      scalaCheck,
      "org.scalamock"      %% "scalamock-scalatest-support"  % "3.2.2",
      scalaTest,
      "ch.qos.logback"      % "logback-classic"              % "1.1.7"
    ) ++ commonDependencies
  )

lazy val coreMacros = Project("aws2scala-core-macros", file("aws2scala-core-macros"))
  .dependsOn(testSupport % "test->test")
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Macros and libraries to enable Akka stream support in aws2scala",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"  %% "akka-stream-testkit"  % akka                % "test",
      "org.scala-lang"      % "scala-reflect"        % scalaVersion.value
    ) ++ commonDependencies
  )

lazy val core = Project("aws2scala-core", file("aws2scala-core"))
  .dependsOn(coreMacros)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Core library for aws2scala",
    libraryDependencies += "com.typesafe" % "config" % "1.3.0"
  )

lazy val coreTestSupport = Project("aws2scala-core-test-support", file("aws2scala-core-test-support"))
  .dependsOn(core, testSupport)
  .settings(
    commonSettings,
    noPublishingSettings,
    description := "Additional aws2scala test support that depends on the core library"
  )

lazy val coreTestkit = Project("aws2scala-core-testkit", file("aws2scala-core-testkit"))
  .dependsOn(core)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Test utility library for aws2scala-core",
    libraryDependencies += scalaCheck
  )

lazy val coreTests = Project("aws2scala-core-tests", file("aws2scala-core-tests"))
  .dependsOn(
    core             % "test",
    coreTestSupport  % "test",
    coreTestkit      % "test",
    testSupport      % "test->test"
  )
  .settings(
    commonSettings,
    noPublishingSettings,
    description := "Test suite for aws2scala-core",
    libraryDependencies ++= Seq(
      sprayJson            % "test",
      awsDependency("s3")  % "test"
    )
  )

lazy val cloudFormation = Project("aws2scala-cloudformation", file("aws2scala-cloudformation"))
  .dependsOn(core, testSupport % "test->test", coreTestSupport % "test")
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Client for AWS CloudFormation",
    libraryDependencies ++= Seq(
      awsDependency("cloudformation"),
      sprayJson  % "test",
      cftg       % "test"
    )
  )

lazy val ec2 = Project("aws2scala-ec2", file("aws2scala-ec2"))
  .dependsOn(core)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Client for Amazon Elastic Cloud Compute (EC2)",
    libraryDependencies += awsDependency("ec2")
  )

lazy val ec2Testkit = Project("aws2scala-ec2-testkit", file("aws2scala-ec2-testkit"))
  .dependsOn(ec2, coreTestkit, iamTestkit)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Test utility library for aws2scala-ec2",
    libraryDependencies ++= Seq(
      scalaCheck,
      "org.bouncycastle" % "bcprov-jdk15on" % "1.54",
      "org.bouncycastle" % "bcpkix-jdk15on" % "1.54"
    )
  )

lazy val ec2Tests = Project("aws2scala-ec2-tests", file("aws2scala-ec2-tests"))
  .dependsOn(
    ec2             % "test",
    ec2Testkit      % "test",
    coreTestSupport % "test",
    testSupport     % "test->test"
  )
  .settings(
    commonSettings,
    noPublishingSettings,
    description := "Tests for aws2scala-ec2"
  )

lazy val iam = Project("aws2scala-iam", file("aws2scala-iam"))
  .dependsOn(core)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Client for AWS Identity and Access Management (IAM)",
    libraryDependencies += awsDependency("iam")
  )

lazy val iamTestkit = Project("aws2scala-iam-testkit", file("aws2scala-iam-testkit"))
  .dependsOn(iam, coreTestkit)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Test utility library for aws2scala-iam",
    libraryDependencies += scalaCheck
  )

lazy val iamTests = Project("aws2scala-iam-tests", file("aws2scala-iam-tests"))
  .dependsOn(
    iam             % "test",
    iamTestkit      % "test",
    coreTestSupport % "test",
    testSupport     % "test->test"
  )
  .settings(
    commonSettings,
    noPublishingSettings,
    description := "Test suite for aws2scala-iam"
  )

lazy val kms = Project("aws2scala-kms", file("aws2scala-kms"))
  .dependsOn(core, testSupport % "test->test", coreTestSupport % "test")
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Client for AWS Key Management Service (KMS)",
    libraryDependencies += awsDependency("kms")
  )

lazy val rds = Project("aws2scala-rds", file("aws2scala-rds"))
  .dependsOn(core, testSupport % "test->test", coreTestSupport % "test")
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Client for Amazon Relational Database Service (RDS)",
    libraryDependencies += awsDependency("rds")
  )

lazy val s3 = Project("aws2scala-s3", file("aws2scala-s3"))
  .dependsOn(core)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Client for Amazon Simple Storage Service (S3)",
    libraryDependencies += awsDependency("s3")
  )

lazy val s3Testkit = Project("aws2scala-s3-testkit", file("aws2scala-s3-testkit"))
  .dependsOn(s3, coreTestkit)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Test utility library for aws2scala-s3",
    libraryDependencies += scalaCheck
  )

lazy val s3Tests = Project("aws2scala-s3-tests", file("aws2scala-s3-tests"))
  .dependsOn(
    s3              % "test",
    s3Testkit       % "test",
    coreTestSupport % "test",
    testSupport     % "test->test"
  )
  .settings(
    commonSettings,
    noPublishingSettings,
    description := "Tests for aws2scala-s3",
    libraryDependencies += sprayJson
  )

lazy val sns = Project("aws2scala-sns", file("aws2scala-sns"))
  .dependsOn(core)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Client for Amazon Simple Notification Service (SNS)",
    libraryDependencies += awsDependency("sns")
  )

lazy val snsTestkit = Project("aws2scala-sns-testkit", file("aws2scala-sns-testkit"))
  .dependsOn(sns, coreTestkit, iamTestkit)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Test utility library for aws2scala-sns",
    libraryDependencies ++= Seq(scalaCheck, sprayJson)
  )

lazy val snsTests = Project("aws2scala-sns-tests", file("aws2scala-sns-tests"))
  .dependsOn(
    sns             % "test",
    snsTestkit      % "test",
    coreTestSupport % "test",
    testSupport     % "test->test"
  )
  .settings(
    commonSettings,
    noPublishingSettings,
    description := "Tests for aws2scala-sns"
  )

lazy val sts = Project("aws2scala-sts", file("aws2scala-sts"))
  .dependsOn(core)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Client for AWS Security Token Service (STS)",
    libraryDependencies += awsDependency("sts")
  )

lazy val stsTestkit = Project("aws2scala-sts-testkit", file("aws2scala-sts-testkit"))
  .dependsOn(sts, coreTestkit, iamTestkit)
  .settings(
    commonSettings,
    bintrayPublishingSettings,
    description := "Test utility library for aws2scala-sts",
    libraryDependencies += scalaCheck
  )

lazy val stsTests = Project("aws2scala-sts-tests", file("aws2scala-sts-tests"))
  .dependsOn(
    sts             % "test",
    stsTestkit      % "test",
    coreTestSupport % "test",
    testSupport     % "test->test"
  )
  .settings(
    commonSettings,
    noPublishingSettings,
    description := "Tests for aws2scala-sts"
  )

lazy val integrationTests = Project("aws2scala-integration-tests", file("aws2scala-integration-tests"))
  .dependsOn(core, testSupport, cloudFormation, ec2, iam, kms, rds, s3, sns, sts)
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    noPublishingSettings,
    Defaults.itSettings,
    description := "Integration test suite for aws2scala",
    libraryDependencies ++= Seq(
      cftg                                   % "it",
      "commons-io"  % "commons-io"  % "2.4"  % "it"
    )
  )

lazy val aws2scala = (project in file("."))
  .aggregate(
    testSupport,
    coreMacros, core, coreTestSupport, coreTests, coreTestkit,
    cloudFormation,
    ec2, ec2Testkit, ec2Tests,
    kms,
    iam, iamTestkit, iamTests,
    rds,
    s3, s3Testkit, s3Tests,
    sns, snsTestkit, snsTests,
    sts, stsTestkit, stsTests,
    integrationTests)
  .settings(
    commonSettings,
    noPublishingSettings,
    // unidoc
    unidocSettings,
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(testSupport, coreTestSupport)
  )
