// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.watch;

import aws.proserve.bcs.dr.lambda.BoolHandler;
import com.amazonaws.services.lambda.runtime.Context;

public class Deploy implements BoolHandler<String> {

    @Override
    public boolean handleRequest(String region, Context context) {


        return false;
    }
}
