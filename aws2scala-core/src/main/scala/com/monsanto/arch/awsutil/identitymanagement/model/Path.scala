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
  /** Constant for an empty path. */
  val empty: Path = Path(Seq.empty)

  /** Utility for extracting/creating a `Path` from a string. */
  object fromPathString {
    /** Creates a `Path` by parsing a string.
      *
      * @throws java.lang.IllegalArgumentException if `pathString` cannot be parsed
      */
    def apply(pathString: String): Path = {
      pathString match {
        case Path.fromPathString(p) ⇒ p
        case _                  ⇒ throw new IllegalArgumentException(s"‘$pathString’ cannot be parsed as a valid path.")
      }
    }

    /** Extracts a `Path` from a path string. */
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
