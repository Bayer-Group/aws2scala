package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.amazonaws.util.json.JSONArray
import com.monsanto.arch.awsutil.auth.policy.PolicyJsonSupport._
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.UtilGen
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import spray.json.{JsArray, JsNull, JsObject, JsString, JsValue, JsonParser}

class PolicyJsonSupportSpec extends FreeSpec {
  TestAction.registerActions()

  "the PolicyJsonSupport should" - {
    "properly serialise" - {
      "a principal set that" - {
        "is empty" in {
          principalsToJson(Set.empty) shouldBe None
        }

        "is all principals set" in {
          principalsToJson(Statement.allPrincipals) shouldBe Some("*")
        }

        "contains a single principal" in {
          forAll { principal: Principal ⇒
            val expected = JsObject(principal.provider → JsString(principal.id))
            val result = JsonParser(principalsToJson(Set(principal)).get.toString)
            result shouldBe expected
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

            val result = JsonParser(principalsToJson(principals).get.toString)

            result shouldBe expected
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
            val result = JsonParser(principalsToJson(principals).get.toString)

            result shouldBe expected
          }
        }
      }

      "an action list that" - {
        "is empty" in {
          actionsToJson(Seq.empty) shouldBe None
        }

        "is the all actions sequence" in {
          actionsToJson(Statement.allActions) shouldBe Some("*")
        }

        "contains a single action" in {
          forAll { action: Action ⇒
            val result = actionsToJson(Seq(action))
            val expected = Some(action.name)
            result shouldBe expected
          }
        }

        "contains multiple actions" in {
          val genActions =
            for {
              first ← arbitrary[Action]
              rest ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Action])
            } yield first :: rest

          forAll(genActions) { actions ⇒
            val result = JsonParser(actionsToJson(actions).get.toString)
            val expected = JsArray(actions.map(a ⇒ JsString(a.name)): _*)
            result shouldBe expected
          }
        }
      }

      "a resource list that" - {
        "is empty" in {
          resourcesToJson(Seq.empty) shouldBe None
        }

        "is the all resources list" in {
          resourcesToJson(Statement.allResources) shouldBe Some("*")
        }

        "contains a single resource" in {
          forAll { resource: Resource ⇒
            resourcesToJson(Seq(resource)) shouldBe Some(resource.id)
          }
        }

        "contains more than one resource" in {
          val genResources =
            for {
              first ← arbitrary[Resource]
              rest ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Resource])
            } yield first :: rest

          forAll(genResources) { resources ⇒
            val result = JsonParser(resourcesToJson(resources).get.toString)
            val expected = JsArray(resources.map(r ⇒ JsString(r.id)): _*)
            result shouldBe expected
          }
        }
      }

      "a condition set that" - {
        "is empty" in {
          conditionsToJson(Set.empty) shouldBe None
        }

        "contains a single condition with a single value" - {
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

            val result = JsonParser(conditionsToJson(Set(condition)).get.toString)

            result shouldBe expected
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

            val result = JsonParser(conditionsToJson(Set(condition)).get.toString)

            result shouldBe expected
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

            val result = JsonParser(conditionsToJson(conditions).get.toString)

            result shouldBe expected
          }
        }

        "merges two conditions with the same type and key" in {
          val mergeableConditions =
            (
              for {
                condition1 ← arbitrary[Condition.StringCondition]
                condition2 ← arbitrary[Condition.StringCondition]
                  .suchThat(c ⇒ c.comparisonValues != condition1.comparisonValues)
                  .map(c ⇒ c.copy(
                    stringComparisonType = condition1.stringComparisonType,
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

            val result = JsonParser(conditionsToJson(conditions).get.toString)

            result shouldBe expected
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
          //   Message: {"ArnNotEquals":{"aws:SourceArn":["arn:aws:AwsCodeDeploy:ap-southeast-1:*:*","arn:aws:AwsStorageGateway:us-west-2:158864837822:*","arn:aws:AmazonCognitoSync:ap-northeast-2:190412167942:*","arn:aws:AwsWAF:ap-southeast-2:399393080574:sgz9qc8EiofhW0eqrolgewnql572","arn:aws:AwsKMS:cn-north-1:*:*","arn:aws:AwsSTS:us-west-2:*:*","arn:aws:AwsCloudFormation:us-east-1:*:zhinussm","arn:aws:AutoScaling:cn-north-1:*:*"]},"DateLessThanEqualsIfExists":{"t3vrjs7yhn4midoyawhfjkuxwtvqalsU2zymmjhpwrty0u2MzdwpndadrCruy":["-179800329-10-02T21:39:56.923Z","-175766714-01-18T18:41:36.766Z"]}}
          //            {"ArnNotEquals":{"aws:SourceArn":["arn:aws:AwsCodeDeploy:ap-southeast-1:*:*","arn:aws:AwsStorageGateway:us-west-2:158864837822:*","arn:aws:AmazonCognitoSync:ap-northeast-2:190412167942:*","arn:aws:AwsWAF:ap-southeast-2:399393080574:sgz9qc8EiofhW0eqrolgewnql572","arn:aws:AwsCodeDeploy:ap-southeast-1:*:*","arn:aws:AwsKMS:cn-north-1:*:*","arn:aws:AwsSTS:us-west-2:*:*","arn:aws:AwsCloudFormation:us-east-1:*:zhinussm","arn:aws:AutoScaling:cn-north-1:*:*"]},"DateLessThanEqualsIfExists":{"t3vrjs7yhn4midoyawhfjkuxwtvqalsU2zymmjhpwrty0u2MzdwpndadrCruy":["-179800329-10-02T21:39:56.923Z","-175766714-01-18T18:41:36.766Z"]}},
          //        and {"ArnNotEquals":{"aws:SourceArn":["arn:aws:AwsCodeDeploy:ap-southeast-1:*:*","arn:aws:AwsStorageGateway:us-west-2:158864837822:*","arn:aws:AmazonCognitoSync:ap-northeast-2:190412167942:*","arn:aws:AwsWAF:ap-southeast-2:399393080574:sgz9qc8EiofhW0eqrolgewnql572","arn:aws:AwsKMS:cn-north-1:*:*","arn:aws:AwsSTS:us-west-2:*:*","arn:aws:AwsCloudFormation:us-east-1:*:zhinussm","arn:aws:AutoScaling:cn-north-1:*:*"]},"DateLessThanEqualsIfExists":{"t3vrjs7yhn4midoyawhfjkuxwtvqalsU2zymmjhpwrty0u2MzdwpndadrCruy":["-179800329-10-02T21:39:56.923Z","-175766714-01-18T18:41:36.766Z"]}}
          //            {"ArnNotEquals":{"aws:SourceArn":["arn:aws:AwsCodeDeploy:ap-southeast-1:*:*","arn:aws:AwsStorageGateway:us-west-2:158864837822:*","arn:aws:AmazonCognitoSync:ap-northeast-2:190412167942:*","arn:aws:AwsWAF:ap-southeast-2:399393080574:sgz9qc8EiofhW0eqrolgewnql572","arn:aws:AwsCodeDeploy:ap-southeast-1:*:*","arn:aws:AwsKMS:cn-north-1:*:*","arn:aws:AwsSTS:us-west-2:*:*","arn:aws:AwsCloudFormation:us-east-1:*:zhinussm","arn:aws:AutoScaling:cn-north-1:*:*"]},"DateLessThanEqualsIfExists":{"t3vrjs7yhn4midoyawhfjkuxwtvqalsU2zymmjhpwrty0u2MzdwpndadrCruy":["-179800329-10-02T21:39:56.923Z","-175766714-01-18T18:41:36.766Z"]}}

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

            val result = JsonParser(conditionsToJson(conditions).get.toString)

            result should (equal (toJson(fwdConditions)) or equal (toJson(revConditions)))
          }
        }
      }

      "a mostly empty Statement" in {
        forAll { effect: Statement.Effect ⇒
          val statement = Statement(None, Set.empty, effect, Seq.empty, Seq.empty, Set.empty)

          val expected = JsObject("Effect" → JsString(effect.name))
          val result = JsonParser(statementToJson(statement).toString)

          result shouldBe expected
        }
      }

      "a mostly empty Policy" in {
        forAll { effect: Statement.Effect ⇒
          val policy = Policy(None, None, Seq(Statement(None, Set.empty, effect, Seq.empty, Seq.empty, Set.empty)))

          val expected =
            JsObject(
              "Statement" → JsArray(JsObject("Effect" → JsString(effect.name)))
            )

          val result = JsonParser(policyToJson(policy).toString)

          result shouldBe expected
        }
      }
    }

    "properly deserialise" - {
      "an unknown action" in {
        jsonToActions(Some("foo")) shouldBe Seq(Action.NamedAction("foo"))
      }

      "a list containing an unknown action" in {
        forAll { actions: Seq[Action] ⇒
          whenever(actions != Statement.allActions) {
            val json = JsArray((actions.map(a ⇒ JsString(a.name)) :+ JsString("foo")).toVector).toString
            val jsonArray = new JSONArray(json)

            jsonToActions(Some(jsonArray)) shouldBe actions :+ Action.NamedAction("foo")
          }
        }
      }
    }

    "round-trip" - {
      "principal sets" in {
        forAll { principals: Set[Principal] ⇒
          jsonToPrincipals(principalsToJson(principals)) shouldBe principals
        }
      }

      "action lists" in {
        forAll { actions: Seq[Action] ⇒
          jsonToActions(actionsToJson(actions)) shouldBe actions
        }
      }

      "resource lists" in {
        forAll { resources: Seq[Resource] ⇒
          jsonToResources(resourcesToJson(resources)) shouldBe resources
        }
      }

      "condition sets" in {
        forAll { conditions: Set[Condition] ⇒
          jsonToConditions(conditionsToJson(conditions)) shouldBe conditions
        }
      }

      "arbitrary statements" in {
        forAll { statement: Statement ⇒
          jsonToStatement(statementToJson(statement)) shouldBe statement
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
        // generate a policy with unique condition types
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
