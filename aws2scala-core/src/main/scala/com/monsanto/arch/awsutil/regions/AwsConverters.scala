package com.monsanto.arch.awsutil.regions

import com.amazonaws.{regions ⇒ aws}

/** Provides converters to/from AWS types for the regions package. */
object AwsConverters {
  implicit class AwsRegion(val region: aws.Regions) extends AnyVal {
    def asScala: Region =
      Region.unapply(region.getName)
        .getOrElse(throw new IllegalArgumentException(s"Could not find Scala equivalent for $region"))
  }

  implicit class ScalaRegion(val region: Region) extends AnyVal {
    def asAws: aws.Regions = aws.Regions.fromName(region.name)
  }
}
