// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.vpc;

import aws.proserve.bcs.dr.dynamo.DynamoConstants;
import aws.proserve.bcs.dr.lambda.BoolHandler;
import aws.proserve.bcs.dr.vpc.DaggerVpcComponent;
import aws.proserve.bcs.dr.vpc.model.Request;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.lambda.runtime.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

public class CheckVpcReplicated implements BoolHandler<Request> {

    @Override
    public boolean handleRequest(Request request, Context context) {
        return DaggerVpcComponent.builder().build().checkVpcReplicated().check(request);
    }

    @Singleton
    public static class Worker {
        private final DynamoDB dynamoDB;

        @Inject
        Worker(DynamoDB dynamoDB) {
            this.dynamoDB = dynamoDB;
        }

        private boolean check(Request request) {
            final var table = dynamoDB.getTable(DynamoConstants.TABLE_VPC);
            final var sourceVpc = request.getSource();
            for (var item : table.query(DynamoConstants.KEY_ID, sourceVpc.getVpcId())) {
                return sourceVpc.getRegion().equals(item.getString(DynamoConstants.KEY_SOURCE_REGION))
                        && request.getTarget().getRegion().equals(item.getString(DynamoConstants.KEY_TARGET_REGION));
            }

            return false;
        }
    }
}
