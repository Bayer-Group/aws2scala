package com.monsanto.arch.awsutil.test_support

object TestDefaults {
  val Tags = Map(
    "mon:project" → "aws2scala",
    "mon:keep-alive" → "false",
    "mon:group" → "ITSA",
    "mon:owner" → System.getProperty("user.name"),
    "mon:environment" → "automated-testing",
    "mon:cost-center" → "5180-9130-SLR74733"
  )
}
