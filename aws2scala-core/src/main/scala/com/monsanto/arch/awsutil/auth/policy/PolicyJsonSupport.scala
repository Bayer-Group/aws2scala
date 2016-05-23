package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.util.json.{JSONArray, JSONObject}

import scala.collection.JavaConverters._

private[awsutil] object PolicyJsonSupport {
  def policyToJson(policy: Policy): String = {
    val jsonObject = new JSONObject()
    jsonObject.putOpt("Version", policy.version.map(_.id).orNull)
    jsonObject.putOpt("Id", policy.id.orNull)
    jsonObject.put("Statement", policy.statements.map(statementToJson).asJavaCollection)
    jsonObject.toString
  }

  def jsonToPolicy(jsonString: String): Policy = {
    val json = new JSONObject(jsonString)
    val version = Option(json.optString("Version", null)).map(Policy.Version.apply)
    val id = Option(json.optString("Id", null))
    val statements = json.getJSONArray("Statement").asScala[JSONObject].map(jsonToStatement).toList
    Policy(version, id, statements)
  }

  def statementToJson(statement: Statement): JSONObject = {
    val jsonObject = new JSONObject
    jsonObject.putOpt("Sid", statement.id.orNull)
    jsonObject.putOpt("Principal", principalsToJson(statement.principals).orNull)
    jsonObject.put("Effect", statement.effect.name)
    jsonObject.putOpt("Action", actionsToJson(statement.actions).orNull)
    jsonObject.putOpt("Resource", resourcesToJson(statement.resources).orNull)
    jsonObject.putOpt("Condition", conditionsToJson(statement.conditions).orNull)
    jsonObject
  }

  def jsonToStatement(jsonObject: JSONObject): Statement = {
    val sid  = Option(jsonObject.optString("Sid", null))
    val principals = jsonToPrincipals(Option(jsonObject.opt("Principal")))
    val effect = Statement.Effect(jsonObject.getString("Effect"))
    val actions = jsonToActions(Option(jsonObject.opt("Action")))
    val resources = jsonToResources(Option(jsonObject.opt("Resource")))
    val conditions = jsonToConditions(Option(jsonObject.opt("Condition").asInstanceOf[JSONObject]))
    Statement(sid, principals, effect, actions, resources, conditions)
  }

  def principalsToJson(principals: Set[Principal]): Option[AnyRef] = {
    if (principals.isEmpty) {
      None
    } else if (principals == Statement.allPrincipals) {
      Some("*")
    } else {
      val jsonObject = new JSONObject()
      principals.groupBy(_.provider).foreach { grouped ⇒
        val provider = grouped._1
        val ids = grouped._2.map(_.id).toList
        ids match {
          case id :: Nil ⇒
            jsonObject.put(provider, id)
          case _ ⇒
            jsonObject.put(provider, new JSONArray(ids.asJavaCollection))
        }
      }
      Some(jsonObject)
    }
  }

  def jsonToPrincipals(jsonObject: Option[AnyRef]): Set[Principal] = {
    jsonObject match {
      case None ⇒
        Set.empty
      case Some("*") ⇒
        Statement.allPrincipals
      case Some(jo: JSONObject) ⇒
        jo.asScala.flatMap { entry ⇒
          val (provider, value) = entry
          value match {
            case id: String ⇒
              Seq(Principal(provider, id))
            case ids: JSONArray ⇒
              ids.asScala[String].map(id ⇒ Principal(provider, id))
          }
        }.toSet
      case _ ⇒ throw new IllegalArgumentException(s"$jsonObject is not a valid principals JSON value.")
    }
  }

  def actionsToJson(actions: Seq[Action]): Option[AnyRef] =
    actions.toList match {
      case Nil           ⇒ None
      case action :: Nil ⇒ Some(action.name)
      case _             ⇒ Some(new JSONArray(actions.map(_.name).asJavaCollection))
    }

  def jsonToActions(json: Option[AnyRef]): Seq[Action] = {
    def getAction(name: String): Action =
      name match {
        case Action.fromName(action) ⇒ action
        case _                       ⇒ Action.NamedAction(name)
      }

    json match {
      case None                   ⇒ Seq.empty
      case Some(name: String)     ⇒ Seq(getAction(name))
      case Some(names: JSONArray) ⇒ names.asScala[String].map(getAction)
      case Some(x)                ⇒ throw new IllegalArgumentException(s"$x is not a valid actions JSON value.")
    }
  }

  def resourcesToJson(resources: Seq[Resource]): Option[AnyRef] =
    resources.toList match {
      case Nil             ⇒ None
      case resource :: Nil ⇒ Some(resource.id)
      case _               ⇒ Some(new JSONArray(resources.map(_.id).asJavaCollection))
    }

  def jsonToResources(json: Option[AnyRef]): Seq[Resource] =
    json match {
      case None                     ⇒ Seq.empty
      case Some(id: String)         ⇒ Seq(Resource(id))
      case Some(jsArray: JSONArray) ⇒ jsArray.asScala[String].map(Resource(_))
      case Some(x)                  ⇒ throw new IllegalArgumentException(s"$x is not a valid resources JSON value.")
    }

  def conditionsToJson(conditions: Set[Condition]): Option[JSONObject] = {
    if (conditions.isEmpty) {
      None
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
      val result = new JSONObject()
      comparisonValuesByTypeAndKey.foreach { typeEntry ⇒
        val (comparisonType, byTypeEntry) = typeEntry
        val typeResult = new JSONObject()
        byTypeEntry.foreach { keyEntry ⇒
          val (key, values) = keyEntry
          values match {
            case Nil ⇒
              // maybe do something?
            case value :: Nil ⇒
              typeResult.put(key, value)
            case _ ⇒
              val jsArray = new JSONArray()
              values.foreach(v ⇒ jsArray.put(v))
              typeResult.put(key, jsArray)
          }
        }
        result.put(comparisonType, typeResult)
      }
      Some(result)
    }
  }

  def jsonToConditions(json: Option[JSONObject]): Set[Condition] =
    json match {
      case None ⇒ Set.empty
      case Some(jsonObject) ⇒
        jsonObject.asScala.flatMap { typeMapping ⇒
          val (comparisonType, keysAndValues: JSONObject) = typeMapping
          keysAndValues.asScala.map { keyAndValues ⇒
            val (key, jsonValues) = keyAndValues
            jsonValues match {
              case value: String ⇒
                Condition(key, comparisonType, Seq(value))
              case values: JSONArray ⇒
                Condition(key, comparisonType, values.asScala[String].toList)
            }
          }
        }.toSet
    }

  private implicit class JsonArrayConverter(val array: JSONArray) extends AnyVal {
    def asScala[T]: Seq[T] = for (i ← 0.until(array.length())) yield array.get(i).asInstanceOf[T]
  }

  private implicit class JsonObjectConverter(val jsObject: JSONObject) extends AnyVal {
    def asScala: Seq[(String,AnyRef)] = {
      val names = jsObject.names()
      for (i ← 0.until(jsObject.length())) yield {
        val key = names.get(i).toString
        val value = jsObject.get(key)
        (key, value)
      }
    }
  }
}
