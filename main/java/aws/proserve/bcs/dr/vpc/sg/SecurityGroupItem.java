// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.sg;

import aws.proserve.bcs.dr.vpc.model.BaseItem;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.model.Type;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.ec2.model.SecurityGroup;

public class SecurityGroupItem extends BaseItem<SecurityGroup> {

    public SecurityGroupItem() {
    }

    public SecurityGroupItem(Request request, SecurityGroup source, SecurityGroup target) {
        super(source.getGroupId(), target.getGroupId(),
                request, Type.SECURITY_GROUP, source, target);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public SecurityGroup getSource() {
        return super.getSource();
    }

    @Override
    public void setSource(SecurityGroup source) {
        super.setSource(source);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public SecurityGroup getTarget() {
        return super.getTarget();
    }

    @Override
    public void setTarget(SecurityGroup target) {
        super.setTarget(target);
    }

    public static class TypeConverter extends BaseItem.TypeConverter<SecurityGroup> {
        public TypeConverter() {
            super(SecurityGroup.class);
        }
    }
}
