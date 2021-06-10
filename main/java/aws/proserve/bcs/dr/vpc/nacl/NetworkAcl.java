// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.nacl;

import aws.proserve.bcs.dr.lambda.VoidHandler;
import aws.proserve.bcs.dr.vpc.VpcComponent;
import aws.proserve.bcs.dr.vpc.model.SubnetRequest;
import com.amazonaws.services.lambda.runtime.Context;

public class NetworkAcl implements VoidHandler<SubnetRequest> {

    @Override
    public void handleRequest(SubnetRequest request, Context context) {
        VpcComponent.build(request).networkAclWorker().handleRequest(request, context);
    }
}
