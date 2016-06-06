package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.prop.TableDrivenPropertyChecks.{Table, forAll ⇒ forAllIn}

//noinspection NameBooleanParameters
class ListPoliciesRequestSpec extends FreeSpec with AwsEnumerationBehaviours {
  "ListPoliciesRequest should" - {
    "have an allPolicies constant" in {
      ListPoliciesRequest.allPolicies shouldBe ListPoliciesRequest(false, Path.empty, ListPoliciesRequest.Scope.All)
    }

    "have a localPolicies constant" in {
      ListPoliciesRequest.localPolicies shouldBe ListPoliciesRequest(false, Path.empty, ListPoliciesRequest.Scope.Local)
    }

    "have a convenience method for list policies with a prefix" in {
      forAll { path: Path ⇒
        ListPoliciesRequest.withPrefix(path) shouldBe ListPoliciesRequest(false, path, ListPoliciesRequest.Scope.All)
      }
    }

    "convert to the correct AWS object" in {
      forAll { request: ListPoliciesRequest ⇒
        val awsRequest = request.asAws
        awsRequest.getOnlyAttached shouldBe request.onlyAttached
        awsRequest should have (
          'PathPrefix (request.prefix.pathString),
          'Scope (request.scope.name)
        )
      }
    }
  }

  "ListPoliciesRequest.Scope should" - {
    val scopes = Table("scope", ListPoliciesRequest.Scope.values: _*)

    "have names matching the AWS string" in {
      forAllIn(scopes) { scope ⇒
        scope.name shouldBe scope.asAws.name()
      }
    }

    "round-trip via their name" in {
      forAllIn(scopes) { scope ⇒
        ListPoliciesRequest.Scope.fromName(scope.name) shouldBe theSameInstanceAs (scope)
      }
    }

    "fail to parse arbitrary strings" in {
      val validNames = ListPoliciesRequest.Scope.values.map(_.name).toSet
      forAll { str: String ⇒
        whenever(!validNames.contains(str)) {
          an [IllegalArgumentException] shouldBe thrownBy (ListPoliciesRequest.Scope.fromName(str))
        }
      }
    }

    behave like anAwsEnumeration(
      aws.PolicyScopeType.values(),
      ListPoliciesRequest.Scope.values,
      (_: ListPoliciesRequest.Scope).asAws,
      (_: aws.PolicyScopeType).asScala)
  }
}
