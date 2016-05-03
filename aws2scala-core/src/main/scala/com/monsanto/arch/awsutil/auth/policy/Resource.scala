package com.monsanto.arch.awsutil.auth.policy

/** Represents a resource involved in an access control policy statement.
  * Resources are the service-specific AWS entities owned by your account, such
  * as SQS queues, S3 buckets and objects, and SNS topics.
  *
  * The resource is C in 'A has permission to do B to C when D applies'.
  */
case class Resource(id: String)
