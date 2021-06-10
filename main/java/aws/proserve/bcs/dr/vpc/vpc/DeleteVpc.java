// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.vpc;

import aws.proserve.bcs.dr.dynamo.DynamoConstants;
import aws.proserve.bcs.dr.lambda.VoidHandler;
import aws.proserve.bcs.dr.vpc.DaggerVpcComponent;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.lambda.runtime.Context;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

public class DeleteVpc implements VoidHandler<String> {

    @Override
    public void handleRequest(String vpcId, Context context) {
        DaggerVpcComponent.builder().build().deleteVpc().delete(vpcId);
    }

    @Singleton
    public static class Worker {
        private final DynamoDB dynamoDB;

        @Inject
        Worker(DynamoDB dynamoDB) {
            this.dynamoDB = dynamoDB;
        }

        private void delete(String vpcId) {
            final var table = dynamoDB.getTable(DynamoConstants.TABLE_VPC);
            final var items = table.scan("#s.vpcId = :vpcId",
                    "id",
                    Map.of("#s", "source"),
                    Map.of(":vpcId", vpcId));
            items.forEach(item ->
                    table.deleteItem("id", item.getString("id")));
        }
    }
}
