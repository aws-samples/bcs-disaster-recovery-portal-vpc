// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.igw;

import aws.proserve.bcs.dr.vpc.model.BaseItem;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.model.Type;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.ec2.model.EgressOnlyInternetGateway;

public class EgressIgwItem extends BaseItem<EgressOnlyInternetGateway> {
    public static final String VPC_ID = "EgressOnlyInternetGateway";

    public EgressIgwItem() {
    }

    public EgressIgwItem(Request request, EgressOnlyInternetGateway source, EgressOnlyInternetGateway target) {
        super(source.getEgressOnlyInternetGatewayId(), target.getEgressOnlyInternetGatewayId(),
                request, Type.EGRESS_GATEWAY, source, target);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public EgressOnlyInternetGateway getSource() {
        return super.getSource();
    }

    @Override
    public void setSource(EgressOnlyInternetGateway source) {
        super.setSource(source);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public EgressOnlyInternetGateway getTarget() {
        return super.getTarget();
    }

    @Override
    public void setTarget(EgressOnlyInternetGateway target) {
        super.setTarget(target);
    }

    public static class TypeConverter extends BaseItem.TypeConverter<EgressOnlyInternetGateway> {
        public TypeConverter() {
            super(EgressOnlyInternetGateway.class);
        }
    }
}
