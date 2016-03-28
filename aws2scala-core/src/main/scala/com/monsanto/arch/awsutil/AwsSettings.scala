package com.monsanto.arch.awsutil

import java.util.concurrent.TimeUnit

import com.amazonaws.regions.{Region, Regions}
import com.typesafe.config._

import scala.concurrent.duration.FiniteDuration

class AwsSettings(config: Config) {
  val region: Region = {
    val regionString = config.getString("awsutil.region")
    val enumValue = try {
      Regions.fromName(regionString)
    } catch {
      case e: IllegalArgumentException =>
        throw new ConfigException.BadValue(
          config.getValue("awsutil.region").origin(),
          "awsutil.region",
          s"Unable to parse ‘$regionString’ into a valid AWS region.",
          e)
    }
    Region.getRegion(enumValue)
  }

  object s3 {
    val uploadCheckInterval: FiniteDuration = {
      val path = "awsutil.s3.upload-check-interval"
      val millis = config.getDuration(path, TimeUnit.MILLISECONDS)
      if (millis <= 0) {
        throw new ConfigException.BadValue(
          config.getValue(path).origin(),
          path,
          "The upload check interval must be at least 1 millisecond")
      }
      FiniteDuration(millis, TimeUnit.MILLISECONDS)
    }

    val uploadCheckTimeout: FiniteDuration = {
      val path = "awsutil.s3.upload-check-timeout"
      val millis = config.getDuration(path, TimeUnit.MILLISECONDS)
      if (millis <= uploadCheckInterval.toMillis) {
        throw new ConfigException.BadValue(
          config.getValue(path).origin(),
          path,
          "The upload check timeout must be greater than the interval")
      }
      FiniteDuration(millis, TimeUnit.MILLISECONDS)
    }

    val defaultBucketPolicy: Option[String] = {
      if (config.hasPath("awsutil.s3.default-bucket-policy")) {
        Some(config.getValue("awsutil.s3.default-bucket-policy").render(AwsSettings.JsonRenderer))
      } else {
        None
      }
    }

    val defaultCopyObjectHeaders: Map[String,Object] = getObjectHeaders("awsutil.s3.default-copy-object-headers")

    val defaultPutObjectHeaders: Map[String,Object] = getObjectHeaders("awsutil.s3.default-put-object-headers")

    private def getObjectHeaders(path: String): Map[String, Object] = {
      import scala.collection.JavaConverters._
      if (config.hasPath(path)) {
        config.getConfig(path).entrySet().asScala.map { entry ⇒
          val key = entry.getKey.replaceAll("^\"(.*)\"$", "$1")
          val rawValue = entry.getValue
          val value = rawValue.valueType match {
            case ConfigValueType.BOOLEAN ⇒ rawValue.unwrapped().toString
            case ConfigValueType.STRING ⇒ rawValue.unwrapped()
            case ConfigValueType.NUMBER ⇒ rawValue.unwrapped()
            case ConfigValueType.NULL ⇒ null // apparently, cannot happen
            case ConfigValueType.OBJECT ⇒ null // apparently, cannot happen
            case ConfigValueType.LIST ⇒
              throw new ConfigException.BadValue(
                rawValue.origin(),
                s"$path.$key",
                "An object header may not be a list")
          }
          key → value
        }.toMap
      } else {
        Map.empty
      }
    }

    val parallelism: Int = {
      val path = "awsutil.s3.parallelism"
      val value = config.getValue(path)
      value.valueType() match {
        case ConfigValueType.STRING ⇒
          if (value.unwrapped() != "auto") {
            throw new ConfigException.BadValue(
              value.origin(),
              path,
              s"must be a positive integer or ‘auto’ (got ${value.unwrapped()}")
          }
          AwsSettings.DefaultS3Parallelism
        case ConfigValueType.NUMBER ⇒
          value.unwrapped() match {
            case d: java.lang.Double ⇒
              throw new ConfigException.BadValue(
                value.origin(),
                path,
                s"must be a positive integer or ‘auto’ (got $d, a double)")
            case l: java.lang.Long ⇒
              throw new ConfigException.BadValue(
                value.origin(),
                path,
                s"must be a positive integer or ‘auto’ (got $l, a long)")
            case i: java.lang.Integer ⇒
              if (i > 0) {
                i
              } else {
                throw new ConfigException.BadValue(
                  value.origin(),
                  path,
                  s"must be a positive integer or ‘auto’ (got $i, a non-positive integer)")
              }
          }
        case _ ⇒
          throw new ConfigException.BadValue(
            value.origin(),
            path,
            s"must be a positive integer or ‘auto’ (got ${value.unwrapped()})")
      }
    }

    override def toString =
      s"S3(uploadCheckInterval -> $uploadCheckInterval, uploadCheckTimeout -> $uploadCheckTimeout, " +
        s"defaultBucketPolicy -> $defaultBucketPolicy, defaultCopyObjectHeaders -> $defaultCopyObjectHeaders, " +
        s"defaultPutObjectHeaders -> $defaultPutObjectHeaders, parallelism -> $parallelism)"
  }

  override def toString =
    s"AwsSettings(region -> $region, s3 -> $s3)"

  // force the object to be loaded
  s3
}

object AwsSettings {
  private val JsonRenderer = ConfigRenderOptions.concise().setJson(true)

  lazy val Default = new AwsSettings(ConfigFactory.load())

  /** The default level of parallelism for asynchronous S3 operations. */
  val DefaultS3Parallelism = Runtime.getRuntime.availableProcessors() * 4
}
