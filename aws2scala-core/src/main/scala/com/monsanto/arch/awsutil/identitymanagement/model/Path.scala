package com.monsanto.arch.awsutil.identitymanagement.model

/** Represents a path within an ARN.
  *
  * @param elements the individual path elements
  */
case class Path(elements: Seq[String]) {
  /** Returns a string representing the path. */
  val pathString = if (elements.isEmpty) "/" else elements.mkString("/", "/", "/")
}

object Path {
  /** Allows creation of a `Path` object by parsing a string. */
  def apply(pathString: String): Path = {
    pathString match {
      case Path.fromString(p) ⇒ p
      case _                  ⇒ throw new IllegalArgumentException(s"‘$pathString’ cannot be parsed as a valid path.")
    }
  }

  /** Constant for an empty path. */
  val empty: Path = Path(Seq.empty)

  /** Extractor for getting a `Path` from a string. */
  object fromString {
    def unapply(str: String): Option[Path] = {
      if (str.nonEmpty) {
        str.split("/").toList match {
          case Nil        ⇒ Some(Path.empty)
          case "" :: rest ⇒ Some(Path(rest))
          case _          ⇒ None
        }
      } else {
        None
      }
    }
  }
}
