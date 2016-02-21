package com.monsanto.arch.awsutil.cloudformation.model

import com.amazonaws.services.cloudformation.{model ⇒ aws}

import scala.collection.JavaConverters

case class ValidatedTemplate(description: Option[String],
                             capabilities: Seq[aws.Capability],
                             capabilitiesReason: Option[String],
                             parameters: Seq[aws.TemplateParameter])

object ValidatedTemplate {
  import JavaConverters._

  def apply(result: aws.ValidateTemplateResult): ValidatedTemplate = {
    ValidatedTemplate(
      Option(result.getDescription).filter(_.nonEmpty),
      result.getCapabilities.asScala.map(aws.Capability.fromValue),
      Option(result.getCapabilitiesReason).filter(_.nonEmpty),
      result.getParameters.asScala)
  }
}

