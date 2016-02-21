package com.monsanto.arch.awsutil

import java.nio.ByteBuffer
import java.util.UUID

package object kms {
  /** Given a string, try to intelligently guess how to use it as a key identifier.  Possibilities include:
    *
    * 1. If it can be parsed as a UUID, then treat it as a unique key identifier as is.
    * 1. If it begins with `arn:`, then treat it as a key or alias arn as is.
    * 1. If it begins with `alias/`, then treat it as an alias as is.
    * 1. Otherwise, assume it is an alias name and prepend `alias/`.
    */
  private[kms] def asKeyIdentifier(idOrAlias: String): String = {
    idOrAlias match {
      case UuidParser(uuid) ⇒ uuid
      case arn if arn.startsWith("arn:") ⇒ arn
      case alias if alias.startsWith("alias/") ⇒ alias
      case alias ⇒ s"alias/$alias"
    }
  }

  private object UuidParser {
    def unapply(str: String): Option[String] = {
      try {
        Some(UUID.fromString(str).toString)
      } catch {
        case _: IllegalArgumentException ⇒ None
      }
    }
  }

  /** Utility to extract the bytes out of a byte buffer. */
  private[kms] def toBytes(buffer: ByteBuffer): Array[Byte] = {
    val roBuffer = buffer.asReadOnlyBuffer()
    val array = Array.ofDim[Byte](roBuffer.remaining())
    roBuffer.get(array)
    array
  }
}
