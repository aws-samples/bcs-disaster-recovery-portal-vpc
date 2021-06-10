// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.nat;

import aws.proserve.bcs.dr.vpc.model.BaseItem;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.model.Type;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.ec2.model.NatGateway;

public class NatGatewayItem extends BaseItem<NatGateway> {

    public NatGatewayItem() {
    }

    public NatGatewayItem(Request request, NatGateway source, NatGateway target) {
        super(source.getNatGatewayId(), target.getNatGatewayId(),
                request, Type.NAT_GATEWAY, source, target);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public NatGateway getSource() {
        return super.getSource();
    }

    @Override
    public void setSource(NatGateway source) {
        super.setSource(source);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public NatGateway getTarget() {
        return super.getTarget();
    }

    @Override
    public void setTarget(NatGateway target) {
        super.setTarget(target);
    }

    public static class TypeConverter extends BaseItem.TypeConverter<NatGateway> {
        public TypeConverter() {
            super(NatGateway.class);
        }
    }
}
