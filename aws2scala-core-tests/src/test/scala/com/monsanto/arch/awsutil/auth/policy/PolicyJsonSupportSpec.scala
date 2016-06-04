package com.monsanto.arch.awsutil.auth.policy

import java.io.StringWriter

import com.amazonaws.auth.{policy ⇒ aws}
import com.fasterxml.jackson.core.{JsonFactory, JsonGenerator}
import com.monsanto.arch.awsutil.auth.policy.PolicyJsonSupport._
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.UtilGen
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import spray.json.{JsArray, JsNull, JsObject, JsString, JsValue, pimpString}

class PolicyJsonSupportSpec extends FreeSpec {
  TestAction.registerActions()

  private val factory = new JsonFactory()

  private def withGenerator(f: JsonGenerator ⇒ Unit): String = {
    val out = new StringWriter()
    val generator = factory.createGenerator(out)
    f(generator)
    generator.close()
    out.toString
  }

  "the PolicyJsonSupport should" - {
    "properly serialise" - {
      "a principal set that" - {
        "is empty" in {
          val result = withGenerator(principalsToJson(_, Set.empty))
          result.parseJson shouldBe JsNull
        }

        "is the all principals set" in {
          val result = withGenerator(principalsToJson(_, Statement.allPrincipals))
          result.parseJson shouldBe JsString("*")
        }

        "contains a single principal" in {
          forAll { principal: Principal ⇒
            val expected = JsObject(principal.provider → JsString(principal.id))
            val result = withGenerator(principalsToJson(_, Set(principal)))
            result.parseJson shouldBe expected
          }
        }

        "contains multiple principals with the same provider" in {
          val sameProviderPrincipals =
            (
              for {
                principal1 ← arbitrary[Principal]
                principal2 ← arbitrary[Principal].retryUntil(_.provider == principal1.provider)
              } yield Set(principal1, principal2)
              ).suchThat(_.size == 2)
          forAll(sameProviderPrincipals) { principals ⇒
            val expected = JsObject(principals.head.provider → JsArray(principals.toSeq.map(p ⇒ JsString(p.id)): _*))

            val result = withGenerator(principalsToJson(_, principals))

            result.parseJson shouldBe expected
          }
        }

        "contains multiple principals from different providers" in {
          val twoPrincipals =
            for {
              principal1 ← arbitrary[Principal]
              principal2 ← arbitrary[Principal].retryUntil(_.provider != principal1.provider)
            } yield Set(principal1, principal2)
          forAll(twoPrincipals) { principals ⇒
            val expected = JsObject(principals.map(p ⇒ p.provider → JsString(p.id)).toMap)
            val result = withGenerator(principalsToJson(_, principals))

            result.parseJson shouldBe expected
          }
        }
      }

      "an action list that" - {
        "is empty" in {
          val result = withGenerator(actionsToJson(_, Seq.empty))
          result.parseJson shouldBe JsNull
        }

        "is the all actions sequence" in {
          val result = withGenerator(actionsToJson(_, Statement.allActions))
          result.parseJson shouldBe JsString("*")
        }

        "contains a single action" in {
          forAll { action: Action ⇒
            val result = withGenerator(actionsToJson(_, Seq(action)))
            result.parseJson shouldBe JsString(action.name)
          }
        }

        "contains multiple actions" in {
          val genActions =
            for {
              first ← arbitrary[Action]
              rest ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Action])
            } yield first :: rest

          forAll(genActions) { actions ⇒
            val result = withGenerator(actionsToJson(_, actions))
            val expected = JsArray(actions.map(a ⇒ JsString(a.name)): _*)
            result.parseJson shouldBe expected
          }
        }
      }

      "a resource list that" - {
        "is empty" in {
          val result = withGenerator(resourcesToJson(_, Seq.empty))
          result.parseJson shouldBe JsNull
        }

        "is the all resources list" in {
          val result = withGenerator(resourcesToJson(_, Statement.allResources))
          result.parseJson shouldBe JsString("*")
        }

        "contains a single resource" in {
          forAll { resource: Resource ⇒
            val result = withGenerator(resourcesToJson(_, Seq(resource)))
            result.parseJson shouldBe JsString(resource.id)
          }
        }

        "contains more than one resource" in {
          val genResources =
            for {
              first ← arbitrary[Resource]
              rest ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Resource])
            } yield first :: rest

          forAll(genResources) { resources ⇒
            val result = withGenerator(resourcesToJson(_, resources))
            val expected = JsArray(resources.map(r ⇒ JsString(r.id)): _*)
            result.parseJson shouldBe expected
          }
        }
      }

      "a condition set that" - {
        "is empty" in {
          val result = withGenerator(conditionsToJson(_, Set.empty))
          result.parseJson shouldBe JsNull
        }

        "contains a single condition with a single value" in {
          val singleValueCondition =
            for {
              condition ← arbitrary[Condition]
            } yield {
              Condition.fromParts(condition.key, condition.comparisonType, Seq(condition.comparisonValues.head))
            }
          forAll(singleValueCondition) { condition ⇒
            val expected =
              JsObject(
                condition.comparisonType → JsObject(
                  condition.key → JsString(condition.comparisonValues.head)
                )
              )

            val result = withGenerator(conditionsToJson(_, Set(condition)))

            result.parseJson shouldBe expected
          }
        }

        "contains a single condition with multiple comparison values" in {
          val multiValueCondition =
            for {
              condition ← Gen.sized { n ⇒
                Gen.resize(n.min(10), arbitrary[Condition]).suchThat(_.comparisonValues.distinct.size > 1)
              }
            } yield condition
          forAll(multiValueCondition) { condition ⇒
            val expected =
              JsObject(
                condition.comparisonType → JsObject(
                  condition.key → JsArray(condition.comparisonValues.distinct.map(JsString.apply): _*)
                )
              )

            val result = withGenerator(conditionsToJson(_, Set(condition)))

            result.parseJson shouldBe expected
          }
        }

        "contains two conditions of the same type with different keys" in {
          val sameTypeConditions =
            (
              for {
                condition1 ← arbitrary[Condition.NumericCondition]
                condition2 ← arbitrary[Condition.NumericCondition]
                  .suchThat(c ⇒ c.key != condition1.key)
                  .map(c ⇒ c.copy(
                    numericComparisonType = condition1.numericComparisonType,
                    ignoreMissing = condition1.ignoreMissing))
              } yield Set[Condition](condition1, condition2)
              ).suchThat(s ⇒ s.size == 2 && s.forall(_.comparisonValues.nonEmpty))

          forAll(sameTypeConditions) { conditions ⇒
            val c1 :: c2 :: Nil = conditions.toList
            def valuesToJson(values: Seq[String]): JsValue = {
              values.map(JsString(_)).toList match {
                case Nil ⇒ JsNull
                case v :: Nil ⇒ v
                case vs ⇒ JsArray(vs.toVector)
              }
            }

            val expected =
              JsObject(
                c1.comparisonType → JsObject(
                  c1.key → valuesToJson(c1.comparisonValues),
                  c2.key → valuesToJson(c2.comparisonValues)
                )
              )

            val result = withGenerator(conditionsToJson(_, conditions))

            result.parseJson shouldBe expected
          }
        }

        "merges two conditions with the same type and key" in {
          val mergeableConditions =
            (
              for {
                condition1 ← arbitrary[Condition.NumericCondition]
                condition2 ← arbitrary[Condition.NumericCondition]
                  .suchThat(c ⇒ c.comparisonValues != condition1.comparisonValues)
                  .map(c ⇒ c.copy(
                    numericComparisonType = condition1.numericComparisonType,
                    ignoreMissing = condition1.ignoreMissing,
                    key = condition1.key))
              } yield Set[Condition](condition1, condition2)
              )
              .suchThat(s ⇒ s.size == 2 && s.forall(_.comparisonValues.nonEmpty))

          forAll(mergeableConditions) { conditions ⇒
            val c1 :: c2 :: Nil = conditions.toList

            val expected =
              JsObject(
                c1.comparisonType → JsObject(
                  c1.key → JsArray((c1.comparisonValues ++ c2.comparisonValues).distinct.map(JsString(_)).toVector)
                )
              )

            val result = withGenerator(conditionsToJson(_, conditions))

            result.parseJson shouldBe expected
          }
        }

        "contains two different types of conditions" in {
          val twoDifferentConditionTypes =
            (
              for {
                condition1 ← arbitrary[Condition.ArnCondition]
                condition2 ← arbitrary[Condition.DateCondition]
              } yield Set[Condition](condition1, condition2)
              ).suchThat(s ⇒ s.size == 2)

          forAll(twoDifferentConditionTypes) { conditions ⇒
            val fwdConditions = conditions.toList
            val revConditions = fwdConditions.reverse
            def toJson(conditions: List[Condition]): JsObject = {
              JsObject(
                conditions
                  .groupBy(_.comparisonType)
                  .mapValues { c ⇒
                    assert(c.size == 1)
                    val values = c.head.comparisonValues.map(JsString(_)).toList match {
                      case v :: Nil ⇒ v
                      case vs ⇒ JsArray(vs.toVector)
                    }
                    JsObject(c.head.key → values)
                  }
              )
            }

            val result = withGenerator(conditionsToJson(_, conditions))

            result.parseJson should (equal (toJson(fwdConditions)) or equal (toJson(revConditions)))
          }
        }
      }

      "a mostly empty Statement" in {
        forAll { effect: Statement.Effect ⇒
          val statement = Statement(None, Set.empty, effect, Seq.empty, Seq.empty, Set.empty)

          val expected = JsObject("Effect" → JsString(effect.name))
          val result = withGenerator(statementToJson(_, statement))

          result.parseJson shouldBe expected
        }
      }

      "a mostly empty Policy" in {
        forAll(minSuccessful(10)) { effect: Statement.Effect ⇒
          val policy = Policy(None, None, Seq(Statement(None, Set.empty, effect, Seq.empty, Seq.empty, Set.empty)))

          val expected =
            JsObject(
              "Statement" → JsArray(JsObject("Effect" → JsString(effect.name)))
            )

          val result = policyToJson(policy)

          result.parseJson shouldBe expected
        }
      }
    }

    "properly deserialise" - {
      "an unknown action" in {
        val parser = factory.createParser(JsString("foo").compactPrint)
        jsonToActions(parser) shouldBe Seq(Action.NamedAction("foo"))
      }

      "a list containing an unknown action" in {
        forAll { actions: Seq[Action] ⇒
          whenever(actions != Statement.allActions) {
            val json = JsArray((actions.map(a ⇒ JsString(a.name)) :+ JsString("foo")).toVector).compactPrint
            val parser = factory.createParser(json)

            jsonToActions(parser) shouldBe actions :+ Action.NamedAction("foo")
          }
        }
      }
    }

    "round-trip" - {
      "principal sets" in {
        forAll { principals: Set[Principal] ⇒
          val json = withGenerator(principalsToJson(_, principals))
          val result = jsonToPrincipals(factory.createParser(json))
          result shouldBe principals
        }
      }

      "action lists" in {
        forAll { actions: Seq[Action] ⇒
          val json = withGenerator(actionsToJson(_, actions))
          val result = jsonToActions(factory.createParser(json))
          result shouldBe actions
        }
      }

      "resource lists" in {
        forAll { resources: Seq[Resource] ⇒
          val json = withGenerator(resourcesToJson(_, resources))
          val result = jsonToResources(factory.createParser(json))
          result shouldBe resources
        }
      }

      "condition sets" in {
        forAll { conditions: Set[Condition] ⇒
          val json = withGenerator(conditionsToJson(_, conditions))
          val result = jsonToConditions(factory.createParser(json))
          result shouldBe conditions
        }
      }

      "arbitrary statements" in {
        forAll { statement: Statement ⇒
          val json = withGenerator(statementToJson(_, statement))
          val result = jsonToStatement(factory.createParser(json))
          result shouldBe statement
        }
      }

      "arbitrary policies" in {
        forAll { policy: Policy ⇒
          jsonToPolicy(policyToJson(policy)) shouldBe policy
        }
      }
    }

    "be equivalent to the AWS serialisation" - {
      val nonBrokenPrincipal = arbitrary[Principal].retryUntil(p ⇒ !p.id.contains("-"))
      val nonBrokenPolicy =
        for {
          policy ← arbitrary[Policy]
          // generate a set of principles that will round-trip
          okPrincipals ← Gen.listOfN(policy.statements.size, UtilGen.listOfSqrtN(nonBrokenPrincipal).map(_.toSet))
          // generate a list of IDs for each statement
          statementIds ← Gen.listOfN(policy.statements.size, Gen.identifier)
        } yield {
          // replace all statement principals with round-trippable ones
          val okStatements = policy.statements.zip(okPrincipals).zip(statementIds).map {
            case ((s, p), i) ⇒ s.copy(principals = p, id = Some(i))
          }
          // create a policy that with the OK statements and with the latest policy version
          policy.copy(statements = okStatements, version = Some(Policy.Version.`2012-10-17`))
        }

      "when serialising" in {
        forAll(nonBrokenPolicy) { policy ⇒
          val json = policyToJson(policy)
          val awsPolicy = aws.Policy.fromJson(json)

          awsPolicy.asScala shouldBe policy
        }
      }

      "when deserialising" in {
        forAll(nonBrokenPolicy) { policy ⇒
          val json = policy.asAws.toJson
          val fromJson = jsonToPolicy(json)

          fromJson shouldBe policy
        }
      }
    }
  }
}
