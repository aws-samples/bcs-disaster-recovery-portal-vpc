// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.dhcp;

import aws.proserve.bcs.dr.lambda.VoidHandler;
import aws.proserve.bcs.dr.vpc.VpcComponent;
import aws.proserve.bcs.dr.vpc.model.Request;
import com.amazonaws.services.lambda.runtime.Context;

public class Dhcp implements VoidHandler<Request> {

    @Override
    public void handleRequest(Request request, Context context) {
        VpcComponent.build(request).dhcpWorker().handleRequest(request, context);
    }
}
