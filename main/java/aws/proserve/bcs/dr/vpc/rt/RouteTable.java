// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.rt;

import aws.proserve.bcs.dr.lambda.MapHandler;
import aws.proserve.bcs.dr.vpc.VpcComponent;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.Map;

public class RouteTable implements MapHandler<RouteTableRequest> {

    @Override
    public Map<String, Object> handleRequest(RouteTableRequest request, Context context) {
        return VpcComponent.build(request).routeTableWorker().handleRequest(request, context);
    }
}
