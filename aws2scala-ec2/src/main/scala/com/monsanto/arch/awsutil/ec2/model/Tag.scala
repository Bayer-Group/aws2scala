package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}

case class Tag(key: String, value: String) {
  private[ec2] def toAws: aws.Tag = new aws.Tag().withKey(key).withValue(value)
}

object Tag {
  private[ec2] def toMap(tags: Seq[Tag]): Map[String,String] = tags.map(Tag.unapply(_).get).toMap
  private[ec2] def fromMap(tags: Map[String,String]): Seq[Tag] = tags.map((Tag.apply _).tupled).toSeq

  private[ec2] def fromAws(tag: aws.Tag): Tag = Tag(tag.getKey, tag.getValue)
}
