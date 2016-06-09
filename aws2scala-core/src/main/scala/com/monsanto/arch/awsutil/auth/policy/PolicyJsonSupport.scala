package com.monsanto.arch.awsutil.auth.policy

import java.io.StringWriter

import com.fasterxml.jackson.core._

private[awsutil] object PolicyJsonSupport {
  private val jsonFactory = new JsonFactory

  def policyToJson(policy: Policy): String = {
    val jsonWriter = new StringWriter
    val generator = jsonFactory.createGenerator(jsonWriter)
    try {
      generator.writeStartObject()
      policy.version.foreach(v ⇒ generator.writeStringField("Version", v.id))
      policy.id.foreach(id ⇒ generator.writeStringField("Id", id))
      generator.writeFieldName("Statement")
      generator.writeStartArray()
      policy.statements.foreach(statementToJson(generator, _))
      generator.writeEndArray()
      generator.writeEndObject()
    } finally {
      generator.close()
      jsonWriter.close()
    }
    jsonWriter.toString
  }

  def jsonToPolicy(jsonString: String): Policy = {
    val parser = jsonFactory.createParser(jsonString)
    var builder = PolicyBuilder.newBuilder

    if (parser.nextToken() != JsonToken.START_OBJECT) {
      throwBadToken(parser, "a policy object")
    }
    while (parser.nextToken() != JsonToken.END_OBJECT) {
      parser.getCurrentName match {
        case "Version" ⇒
          if (parser.nextToken() == JsonToken.VALUE_STRING) {
            builder = builder.withVersion(Policy.Version.fromId(parser.getValueAsString))
          } else {
            throwBadToken(parser, "a string policy version")
          }
        case "Id" ⇒
          if (parser.nextToken() == JsonToken.VALUE_STRING) {
            builder = builder.withId(parser.getValueAsString)
          } else {
            throwBadToken(parser, "a string policy identifier")
          }
        case "Statement" ⇒
          if (parser.nextToken() != JsonToken.START_ARRAY) {
            throwBadToken(parser, "a statement array")
          }
          val statements = Seq.newBuilder[Statement]
          while (parser.nextToken() != JsonToken.END_ARRAY) {
            statements += jsonToStatement(parser)
          }
          builder = builder.withStatements(statements.result())
        case x ⇒
          throwBadValue(parser, "Version, Id, or Statement", x)
      }
    }
    builder.result
  }

  def statementToJson(generator: JsonGenerator, statement: Statement): Unit = {
    generator.writeStartObject()
    statement.id.foreach(id ⇒ generator.writeStringField("Sid", id))
    generator.writeFieldIfNonEmpty("Principal", statement.principals, principalsToJson)
    generator.writeStringField("Effect", statement.effect.name)
    generator.writeFieldIfNonEmpty("Action", statement.actions, actionsToJson)
    generator.writeFieldIfNonEmpty("Resource", statement.resources, resourcesToJson)
    generator.writeFieldIfNonEmpty("Condition", statement.conditions, conditionsToJson)
    generator.writeEndObject()
  }

  def jsonToStatement(parser: JsonParser): Statement = {
    if (!(parser.getCurrentToken == JsonToken.START_OBJECT) && !(parser.getCurrentToken == null && parser.nextToken() == JsonToken.START_OBJECT)) {
      throwBadToken(parser, "a statement object")
    }
    var builder = StatementBuilder.newBuilder
    while (parser.nextToken() != JsonToken.END_OBJECT) {
      parser.getCurrentName match {
        case "Sid" ⇒
          if (parser.nextToken() == JsonToken.VALUE_STRING) {
            builder = builder.withSid(parser.getValueAsString)
          } else {
            throwBadToken(parser, "a string statement identifier")
          }
        case "Principal" ⇒
          builder = builder.withPrincipals(jsonToPrincipals(parser))
        case "Effect" ⇒
          if (parser.nextToken() == JsonToken.VALUE_STRING) {
            builder = builder.withEffect(Statement.Effect.fromName(parser.getValueAsString))
          } else {
            throwBadToken(parser, "a string statement effect")
          }
        case "Action" ⇒
          builder = builder.withActions(jsonToActions(parser))
        case "Resource" ⇒
          builder = builder.withResources(jsonToResources(parser))
        case "Condition" ⇒
          builder = builder.withConditions(jsonToConditions(parser))
        case x ⇒
          throwBadValue(parser, "Sid, Principal, Effect, Action, Resource, or Condition", x)
      }
    }
    builder.result
  }

  def principalsToJson(generator: JsonGenerator, principals: Set[Principal]): Unit = {
    if (principals.isEmpty) {
      generator.writeNull()
    } else if (principals == Statement.allPrincipals) {
      generator.writeString("*")
    } else {
      generator.writeStartObject()
      principals.groupBy(_.provider).foreach { grouped ⇒
        val provider = grouped._1
        val ids = grouped._2.map(_.id).toList
        generator.writeCollapsibleStringsField(provider, ids)
      }
      generator.writeEndObject()
    }
  }

  def jsonToPrincipals(parser: JsonParser): Set[Principal] = {
    parser.nextToken() match {
      case JsonToken.VALUE_NULL ⇒
        Set.empty
      case JsonToken.VALUE_STRING ⇒
        parser.getValueAsString match {
          case "*" ⇒ Statement.allPrincipals
          case x ⇒ throwBadValue(parser, "‘*’", x)
        }
      case JsonToken.START_OBJECT ⇒
        val builder = Set.newBuilder[Principal]
        while (parser.nextToken() != JsonToken.END_OBJECT) {
          val provider = parser.getCurrentName
          val ids = parser.readCollapsibleStringValue()
          builder ++= ids.map(id ⇒ Principal.fromProviderAndId(provider, id))
        }
        builder.result()
      case _ ⇒
        throwBadToken(parser, "either null, the string ‘*’, or an object")
    }
  }

  def actionsToJson(generator: JsonGenerator, actions: Seq[Action]): Unit =
    generator.writeCollapsibleStrings(actions.map(_.name))

  def jsonToActions(parser: JsonParser): Seq[Action] =
    parser.readCollapsibleStringValue(true).collect {
      case Action.fromName(action) ⇒ action
      case name                    ⇒ Action.NamedAction(name)
    }

  def resourcesToJson(generator: JsonGenerator, resources: Seq[Resource]): Unit =
    generator.writeCollapsibleStrings(resources.map(_.id))

  def jsonToResources(parser: JsonParser): Seq[Resource] =
    parser.readCollapsibleStringValue(true).map(Resource(_))


  def conditionsToJson(generator: JsonGenerator, conditions: Set[Condition]): Unit =
    if (conditions.isEmpty) {
      generator.writeNull()
    } else {
      val comparisonValuesByTypeAndKey =
        conditions
          .groupBy(_.comparisonType)
          .mapValues { byTypeConditions ⇒
            byTypeConditions
              .groupBy(_.key)
              .mapValues { byTypeAndKeyConditions ⇒
                  byTypeAndKeyConditions.toList.flatMap(_.comparisonValues).distinct
              }
          }
      generator.writeStartObject()
      comparisonValuesByTypeAndKey.foreach { typeEntry ⇒
        val (comparisonType, byTypeEntry) = typeEntry
        generator.writeFieldName(comparisonType)
        generator.writeStartObject()
        byTypeEntry.foreach { keyEntry ⇒
          val (key, values) = keyEntry
          generator.writeCollapsibleStringsField(key, values)
        }
        generator.writeEndObject()
      }
      generator.writeEndObject()
    }

  def jsonToConditions(parser: JsonParser): Set[Condition] =
    parser.nextToken() match {
      case JsonToken.VALUE_NULL ⇒
        Set.empty
      case JsonToken.START_OBJECT ⇒
        val conditions = Set.newBuilder[Condition]
        while(parser.nextToken() != JsonToken.END_OBJECT) {
          val comparisonType = parser.getCurrentName
          if (parser.nextToken() != JsonToken.START_OBJECT) {
            throwBadToken(parser, "an object of key names with comparison values")
          }
          while(parser.nextToken() != JsonToken.END_OBJECT) {
            val key = parser.getCurrentName
            val values = parser.readCollapsibleStringValue()
            conditions += Condition.fromParts(key, comparisonType, values)
          }
        }
        conditions.result()
      case _ ⇒
        throwBadToken(parser, "either null or a condition object")
    }

  implicit class EnhancedJsonGenerator(val generator: JsonGenerator) extends AnyVal {
    def writeCollapsibleStrings(strings: Seq[String]): Unit =
      strings.length match {
        case 0 ⇒ generator.writeNull()
        case 1 ⇒ generator.writeString(strings.head)
        case _ ⇒
          generator.writeStartArray()
          strings.foreach(generator.writeString)
          generator.writeEndArray()
      }

    def writeCollapsibleStringsField(name: String, strings: Seq[String]): Unit = {
      generator.writeFieldName(name)
      writeCollapsibleStrings(strings)
    }

    def writeFieldIfNonEmpty[T <: TraversableOnce[_]](name: String,
                                                      collection: T,
                                                      serializer: (JsonGenerator, T) ⇒ Unit): Unit = {
      if (collection.nonEmpty) {
        generator.writeFieldName(name)
        serializer(generator, collection)
      }
    }
  }

  implicit class EnhancedJsonParser(val parser: JsonParser) extends AnyVal {
    def readCollapsibleStringValue(nullOk: Boolean): Seq[String] = {
      parser.nextToken() match {
        case JsonToken.VALUE_STRING ⇒
          Seq(parser.getValueAsString)
        case JsonToken.START_ARRAY ⇒
          val strings = Seq.newBuilder[String]
          while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.getCurrentToken == JsonToken.VALUE_STRING) {
              strings += parser.getValueAsString
            } else {
              throwBadToken(parser, "a string array value")
            }
          }
          strings.result()
        case JsonToken.VALUE_NULL if nullOk ⇒
          Seq.empty
        case _ ⇒
          if (nullOk) {
            throwBadToken(parser, "null, a string, or an array of strings")
          } else {
            throwBadToken(parser, "a string or an array of strings")
          }
      }
    }

    def readCollapsibleStringValue(): Seq[String] = readCollapsibleStringValue(false)
  }

  private def throwBadToken(jsonParser: JsonParser, expected: String): Nothing = {
    val tokenName = jsonParser.getCurrentToken match {
      case JsonToken.END_ARRAY             ⇒ "the end of an array"
      case JsonToken.END_OBJECT            ⇒ "the end of an object"
      case JsonToken.FIELD_NAME            ⇒ s"the field ‘${jsonParser.getText}’"
      case JsonToken.NOT_AVAILABLE         ⇒ "no available input"
      case JsonToken.START_ARRAY           ⇒ "an array"
      case JsonToken.START_OBJECT          ⇒ "an object"
      case JsonToken.VALUE_EMBEDDED_OBJECT ⇒ "an object"
      case JsonToken.VALUE_FALSE           ⇒ "the boolean value ‘false’"
      case JsonToken.VALUE_NULL            ⇒ "the null value"
      case JsonToken.VALUE_NUMBER_FLOAT    ⇒ "a floating-point number"
      case JsonToken.VALUE_NUMBER_INT      ⇒ "an integer"
      case JsonToken.VALUE_STRING          ⇒ "a string"
      case JsonToken.VALUE_TRUE            ⇒ "the boolean value ‘true’"
    }
    throw new JsonParseException(
      s"Expected $expected but got $tokenName.",
      jsonParser.getTokenLocation)
  }

  private def throwBadValue[T](jsonParser: JsonParser, expected: String, actual: T): Nothing = {
    throw new JsonParseException(
      s"Expected $expected but got $actual.",
      jsonParser.getCurrentLocation)
  }
}
