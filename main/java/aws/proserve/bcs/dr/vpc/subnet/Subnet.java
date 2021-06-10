// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.subnet;

import aws.proserve.bcs.dr.lambda.MapHandler;
import aws.proserve.bcs.dr.vpc.VpcComponent;
import aws.proserve.bcs.dr.vpc.model.Request;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.Map;

public class Subnet implements MapHandler<Request> {

    @Override
    public Map<String, Object> handleRequest(Request request, Context context) {
        return VpcComponent.build(request).subnetWorker().handleRequest(request, context);
    }
}
