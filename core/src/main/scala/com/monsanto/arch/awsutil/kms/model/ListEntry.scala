package com.monsanto.arch.awsutil.kms.model

import com.amazonaws.services.kms.model.{AliasListEntry, KeyListEntry}

case class ListEntry(keyId: String, keyArn: String, aliasName: Option[String], aliasArn: Option[String])

object ListEntry {
  def apply(key: KeyListEntry): ListEntry = ListEntry(key.getKeyId, key.getKeyArn, None, None)

  def apply(key: KeyListEntry, alias: AliasListEntry): ListEntry =
    ListEntry(key.getKeyId, key.getKeyArn, Some(alias.getAliasName.replaceFirst("^alias/", "")),
      Some(alias.getAliasArn))
}
