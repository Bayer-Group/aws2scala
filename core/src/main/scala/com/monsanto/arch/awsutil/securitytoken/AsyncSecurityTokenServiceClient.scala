package com.monsanto.arch.awsutil.securitytoken

import akka.stream.Materializer
import com.monsanto.arch.awsutil.securitytoken.model.{Credentials, AssumeRoleRequest, AssumeRoleResult}

import scala.concurrent.Future

trait AsyncSecurityTokenServiceClient {
  /** Returns a set of temporary security credentials you can use to access AWS resources to which you might not
    * normally have access.
    *
    * If you need to specify a policy, custom duration, or MFA token, or if you need access to the full result from
    * AWS, you will need to use the [[assumeRole(request:* `assumeRole(request)`]] method.
    *
    * @param roleArn the ARN of the role to assume
    * @param sessionName the name of the assumed role session
    * @return temporary security credentials for the assumed role
    */
  def assumeRole(roleArn: String, sessionName: String)(implicit m: Materializer): Future[Credentials]

  /** Returns a set of temporary security credentials you can use to access AWS resources to which you might not
    * normally have access.
    *
    * If you need to specify a policy, custom duration, or MFA token, or if you need access to the full result from
    * AWS, you will need to use the [[assumeRole(request:* `assumeRole(request)`]] method.
    *
    * @param roleArn the ARN of the role to assume
    * @param sessionName the name of the assumed role session
    * @param externalId an external ID to include in the request
    * @return temporary security credentials for the assumed role
    */
  def assumeRole(roleArn: String, sessionName: String, externalId: String)(implicit m: Materializer): Future[Credentials]

  /** Returns a set of temporary security credentials you can use to access AWS resources to which you might not
    * normally have access.
    *
    * If you do not need to specify an MFA token or policy and do not require the full request object, you may wish to
    * use [[assumeRole(roleArn:String,sessionName:String)* `assumeRole(roleArn, sessionName)`]] or
    * [[assumeRole(roleArn:String,sessionName:String,externalId:String)* `assumeRole(roleArn, sessionName, externalId)`]].
    *
    *
    * @param request the full request, consisting of a role ARN, a session name, and other option parameters
    * @return the full result from AWS, which includes the credentials, assumed role information, and the packed policy
    *         size
    */
  def assumeRole(request: AssumeRoleRequest)(implicit m: Materializer): Future[AssumeRoleResult]
}
