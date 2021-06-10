// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.endpoint;

import aws.proserve.bcs.dr.lambda.StringHandler;
import aws.proserve.bcs.dr.vpc.VpcComponent;
import com.amazonaws.services.lambda.runtime.Context;

public class Endpoint implements StringHandler<EndpointRequest> {

    @Override
    public String handleRequest(EndpointRequest request, Context context) {
        return VpcComponent.build(request).endpointWorker().handleRequest(request, context);
    }
}
