// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.sg;

import aws.proserve.bcs.dr.lambda.VoidHandler;
import aws.proserve.bcs.dr.vpc.VpcComponent;
import com.amazonaws.services.lambda.runtime.Context;

public class SecurityGroupRule implements VoidHandler<SecurityGroupRuleRequest> {

    @Override
    public void handleRequest(SecurityGroupRuleRequest request, Context context) {
        VpcComponent.build(request).securityGroupRuleWorker().handleRequest(request, context);
    }
}
