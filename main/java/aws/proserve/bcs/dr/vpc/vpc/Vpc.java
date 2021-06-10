// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.vpc;

import aws.proserve.bcs.dr.lambda.StringHandler;
import aws.proserve.bcs.dr.vpc.VpcComponent;
import aws.proserve.bcs.dr.vpc.model.Request;
import com.amazonaws.services.lambda.runtime.Context;

public class Vpc implements StringHandler<Request> {

    @Override
    public String handleRequest(Request request, Context context) {
        return VpcComponent.build(request).vpcWorker().handleRequest(request, context);
    }

}
