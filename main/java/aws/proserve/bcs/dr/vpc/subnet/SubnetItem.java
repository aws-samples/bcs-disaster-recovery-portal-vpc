// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.subnet;

import aws.proserve.bcs.dr.vpc.model.BaseItem;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.model.Type;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.ec2.model.Subnet;

public class SubnetItem extends BaseItem<Subnet> {

    public SubnetItem() {
    }

    public SubnetItem(Request request, Subnet source, Subnet target) {
        super(source.getSubnetId(), target.getSubnetId(),
                request, Type.SUBNET, source, target);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public Subnet getSource() {
        return super.getSource();
    }

    @Override
    public void setSource(Subnet source) {
        super.setSource(source);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public Subnet getTarget() {
        return super.getTarget();
    }

    @Override
    public void setTarget(Subnet target) {
        super.setTarget(target);
    }

    public static class TypeConverter extends BaseItem.TypeConverter<Subnet> {
        public TypeConverter() {
            super(Subnet.class);
        }
    }
}
