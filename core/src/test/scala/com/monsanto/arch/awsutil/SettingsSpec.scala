package com.monsanto.arch.awsutil

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.Headers
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.config._
import org.scalatest.{FreeSpec, Matchers}
import spray.json.{JsArray, JsObject, JsString, JsonParser}

import scala.concurrent.duration.DurationInt

class SettingsSpec extends FreeSpec {
  import Matchers._
  import SettingsSpec._

  "the Settings class" - {
    "loads the defaults" in {
      val settings = new Settings(ConfigFactory.defaultReference())
      settings.region shouldBe Region.getRegion(Regions.US_EAST_1)
      settings.s3.defaultBucketPolicy shouldBe empty
      settings.s3.defaultCopyObjectHeaders shouldBe empty
      settings.s3.defaultPutObjectHeaders shouldBe empty
      settings.s3.uploadCheckInterval shouldBe 1.second
      settings.s3.uploadCheckTimeout shouldBe 1.minute
      settings.s3.parallelism shouldBe Settings.DefaultS3Parallelism
    }

    "has a readable toString" in {
      val settings = new Settings(ConfigFactory.defaultReference())
      settings.toString shouldBe "Settings(region -> us-east-1, s3 -> S3(uploadCheckInterval -> 1000 milliseconds, " +
        "uploadCheckTimeout -> 60000 milliseconds, defaultBucketPolicy -> None, defaultCopyObjectHeaders -> Map(), " +
        s"defaultPutObjectHeaders -> Map(), parallelism -> ${Settings.DefaultS3Parallelism}))"
    }

    "requires a region to be" - {
      "present" in {
        a [ConfigException.Missing] shouldBe thrownBy(new Settings(ConfigFactory.parseString("")))
      }

      "a valid AWS region name" in {
        a [ConfigException.BadValue] shouldBe thrownBy(new Settings(ConfigFactory.parseString("awsutil.region = 2")))
      }
    }

    "the s3 settings" - {
      def objectHeaderSetting(path: String, headersFrom: Settings ⇒ Map[String,AnyRef]): Unit = {
        "that are sane" in {
          val settings = new Settings(S3AltConfig)
          headersFrom(settings) shouldBe Map(
            Headers.SERVER_SIDE_ENCRYPTION → ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION
          )
        }

        "that are booleans" in {
          val config = ConfigFactory.parseString(s"$path.bool = true").withFallback(MinimalConfig)
          val settings = new Settings(config)
          headersFrom(settings) shouldBe Map(
            "bool" → "true"
          )
        }

        "that are numbers" in {
          val config = ConfigFactory.parseString(s"$path.number = 2").withFallback(MinimalConfig)
          val settings = new Settings(config)
          headersFrom(settings) shouldBe Map(
            "number" → 2
          )
        }

        "cannot be lists" in {
          val config = ConfigFactory.parseString(
            s"""$path {
               |  list = [1, 2, 3]
               |}
             """.stripMargin).withFallback(MinimalConfig)
          a [ConfigException.BadValue] shouldBe thrownBy(new Settings(config))
        }

      }

      "may contain a default bucket policy" in {
        val settings = new Settings(S3AltConfig)
        settings.s3.defaultBucketPolicy shouldBe defined
        JsonParser(settings.s3.defaultBucketPolicy.get) shouldBe S3AltDefaultBucketPolicy
      }

      "may contain default copy-object headers" - {
        behave like objectHeaderSetting("awsutil.s3.default-copy-object-headers", s ⇒ s.s3.defaultCopyObjectHeaders)
      }

      "may contain default put-object headers" - {
        behave like objectHeaderSetting("awsutil.s3.default-put-object-headers", s ⇒ s.s3.defaultPutObjectHeaders)
      }

      "requires a upload check interval to be" - {
        val path = "awsutil.s3.upload-check-interval"

        "present" in {
          val config = MinimalConfig.withoutPath(path)
          a [ConfigException.Missing] shouldBe thrownBy(new Settings(config))
        }

        "at least one millisecond" in {
          val config = ConfigFactory.parseString(s"$path = 1ns").withFallback(MinimalConfig)
          a [ConfigException.BadValue] shouldBe thrownBy(new Settings(config))
        }
      }

      "requires a upload check timeout to be" - {
        val path = "awsutil.s3.upload-check-timeout"

        "present" in {
          val config = MinimalConfig.withoutPath(path)
          a [ConfigException.Missing] shouldBe thrownBy(new Settings(config))
        }

        "larger than the interval" in {
          val config = ConfigFactory.parseString(s"$path = 1s").withFallback(MinimalConfig)
          a [ConfigException.BadValue] shouldBe thrownBy(new Settings(config))
        }
      }

      "requires a parallelism to be" - {
        val path = "awsutil.s3.parallelism"

        "present" in {
          val config = MinimalConfig.withoutPath(path)
          a [ConfigException.Missing] shouldBe thrownBy(new Settings(config))
        }

        "possibly ‘auto’" in {
          val config = ConfigFactory.parseString(s"$path = auto").withFallback(MinimalConfig)
          new Settings(config).s3.parallelism shouldBe Settings.DefaultS3Parallelism
        }

        "not a string that is not ‘auto’" in {
          val config = ConfigFactory.parseString(s"$path = aero").withFallback(MinimalConfig)
          a [ConfigException.BadValue] shouldBe thrownBy(new Settings(config))
        }

        "possibly a positive integer" in {
          val config = ConfigFactory.parseString(s"$path = 3").withFallback(MinimalConfig)
          new Settings(config).s3.parallelism shouldBe 3
        }

        "not a double" in {
          val config = ConfigFactory.parseString(s"$path = 3.14").withFallback(MinimalConfig)
          a [ConfigException.BadValue] shouldBe thrownBy(new Settings(config))
        }

        "not a long" in {
          val config = ConfigFactory.parseString(s"$path = 314159265358979").withFallback(MinimalConfig)
          a [ConfigException.BadValue] shouldBe thrownBy(new Settings(config))
        }

        "not zero" in {
          val config = ConfigFactory.parseString(s"$path = 0").withFallback(MinimalConfig)
          a [ConfigException.BadValue] shouldBe thrownBy(new Settings(config))
        }

        "not negative" in {
          val config = ConfigFactory.parseString(s"$path = -2").withFallback(MinimalConfig)
          a [ConfigException.BadValue] shouldBe thrownBy(new Settings(config))
        }

        "not an array" in {
          val config = ConfigFactory.parseString(s"$path = []").withFallback(MinimalConfig)
          a [ConfigException.BadValue] shouldBe thrownBy(new Settings(config))
        }

        "not a boolean" in {
          val config = ConfigFactory.parseString(s"$path = false").withFallback(MinimalConfig)
          a [ConfigException.BadValue] shouldBe thrownBy(new Settings(config))
        }

        "not an object" in {
          val config = ConfigFactory.parseString(s"$path {}").withFallback(MinimalConfig)
          a [ConfigException.BadValue] shouldBe thrownBy(new Settings(config))
        }
      }
    }
  }
}

object SettingsSpec {
  val S3AltConfig = ConfigFactory.load("s3-alt-settings")

  val MinimalConfig = ConfigFactory.defaultReference().withOnlyPath("awsutil")

  val S3AltDefaultBucketPolicy: JsObject =
    JsObject(
      "Version" → JsString("2012-10-17"),
      "Statement" → JsArray(
        JsObject(
          "Sid" → JsString("DenyNonServerSideEncryptedObjectUploads"),
          "Effect" → JsString("Deny"),
          "Principal" → JsString("*"),
          "Action" → JsString("s3:PutObject"),
          "Resource" → JsString(s"arn:aws:s3:::@BUCKET_NAME@/*"),
          "Condition" → JsObject("StringNotEquals" → JsObject("s3:x-amz-server-side-encryption" → JsArray(JsString("AES256"), JsString("aws-kms")))))))
}
