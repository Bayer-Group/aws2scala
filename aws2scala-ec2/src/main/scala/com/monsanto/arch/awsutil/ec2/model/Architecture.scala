package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.ArchitectureValues
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class Architecture(val toAws: ArchitectureValues) extends AwsEnumeration[ArchitectureValues]

object Architecture extends AwsEnumerationCompanion[Architecture, ArchitectureValues] {
  case object I386 extends Architecture(ArchitectureValues.I386)
  case object X86_64 extends Architecture(ArchitectureValues.X86_64)

  override val values: Seq[Architecture] = Seq(I386, X86_64)
}
