// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.nat;

import aws.proserve.bcs.dr.lambda.MapHandler;
import aws.proserve.bcs.dr.vpc.VpcComponent;
import aws.proserve.bcs.dr.vpc.model.SubnetRequest;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.Map;

public class NatGateway implements MapHandler<SubnetRequest> {

    @Override
    public Map<String, Object> handleRequest(SubnetRequest request, Context context) {
        return VpcComponent.build(request).natWorker().handleRequest(request, context);
    }
}
