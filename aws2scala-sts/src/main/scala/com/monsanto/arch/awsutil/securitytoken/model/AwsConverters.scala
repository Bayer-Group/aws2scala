package com.monsanto.arch.awsutil.securitytoken.model

import com.amazonaws.services.securitytoken.{model ⇒ aws}

object AwsConverters {
  implicit class ScalaAssumeRoleRequest(val request: AssumeRoleRequest) extends AnyVal {
    def asAws: aws.AssumeRoleRequest = {
      val awsRequest = new aws.AssumeRoleRequest()
        .withRoleArn(request.roleArn)
        .withRoleSessionName(request.roleSessionName)
      request.duration.foreach(d ⇒ awsRequest.setDurationSeconds(d.toSeconds.toInt))
      request.externalId.foreach(id ⇒ awsRequest.setExternalId(id))
      request.policy.foreach(p ⇒ awsRequest.setPolicy(p))
      request.mfa.foreach { mfa ⇒
        awsRequest.setSerialNumber(mfa.serialNumber)
        awsRequest.setTokenCode(mfa.tokenCode)
      }
      awsRequest
    }
  }

  implicit class ScalaAssumeRoleResult(val result: AssumeRoleResult) extends AnyVal {
    def asAws: aws.AssumeRoleResult = {
      val awsResult = new aws.AssumeRoleResult
      awsResult.setAssumedRoleUser(result.assumedRoleUser.asAws)
      awsResult.setCredentials(result.credentials.asAws)
      result.packedPolicySize.foreach(pps ⇒ awsResult.setPackedPolicySize(pps))
      awsResult
    }
  }

  implicit class AwsAssumeRoleResult(val result: aws.AssumeRoleResult) extends AnyVal {
    def asScala: AssumeRoleResult =
      AssumeRoleResult(
        result.getAssumedRoleUser.asScala,
        result.getCredentials.asScala,
        Option(result.getPackedPolicySize).map(_.toInt))
  }

  implicit class ScalaAssumedRoleUser(val assumedRoleUser: AssumedRoleUser) extends AnyVal {
    def asAws: aws.AssumedRoleUser =
      new aws.AssumedRoleUser()
        .withArn(assumedRoleUser.arn)
        .withAssumedRoleId(assumedRoleUser.assumedRoleId)
  }

  implicit class AwsAssumedRoleUser(val assumedRoleUser: aws.AssumedRoleUser) extends AnyVal {
    def asScala: AssumedRoleUser = AssumedRoleUser(assumedRoleUser.getArn, assumedRoleUser.getAssumedRoleId)
  }

  implicit class AwsCredentials(val credentials: aws.Credentials) extends AnyVal {
    def asScala: Credentials =
      Credentials(credentials.getAccessKeyId, credentials.getSecretAccessKey, credentials.getSessionToken,
        credentials.getExpiration)
  }

  implicit class ScalaCredentials(val credentials: Credentials) extends AnyVal {
    def asAws: aws.Credentials =
      new aws.Credentials(
        credentials.accessKeyId,
        credentials.secretAccessKey,
        credentials.sessionToken,
        credentials.expiration)
  }
}
