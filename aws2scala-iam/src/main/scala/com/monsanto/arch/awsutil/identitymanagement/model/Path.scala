package com.monsanto.arch.awsutil.identitymanagement.model

private[awsutil] case class Path(elements: Seq[String]) {
  val value = if (elements.isEmpty) "/" else elements.mkString("/", "/", "/")
  override def toString = value
}

private[awsutil] object Path {
  def apply(path: String): Path = {
    path.split("/").toList match {
      case Nil ⇒ Path(Seq.empty)
      case "" :: rest ⇒ Path(rest)
      case _ ⇒ throw new IllegalArgumentException()
    }
  }
}
