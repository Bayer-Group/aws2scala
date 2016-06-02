package com.monsanto.arch.awsutil.kms.model

import com.monsanto.arch.awsutil.auth.policy.Policy

/** Requests creation of a new customer master key (CMK) with an associated alias.
  *
  * @param alias the display name for the CMK.  The `alias/` prefix is not required.
  * @param policy the policy to attach to the CMK
  * @param description an optional description for the CMK
  * @param keyUsage the intended use of the CMK
  */
case class CreateKeyWithAliasRequest(alias: String,
                                     policy: Option[Policy],
                                     description: Option[String],
                                     keyUsage: KeyUsage)
