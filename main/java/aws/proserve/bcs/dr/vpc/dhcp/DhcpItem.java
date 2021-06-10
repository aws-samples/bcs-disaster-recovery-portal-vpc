// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.dhcp;

import aws.proserve.bcs.dr.vpc.model.BaseItem;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.model.Type;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.ec2.model.DhcpOptions;

public class DhcpItem extends BaseItem<DhcpOptions> {

    public DhcpItem() {
    }

    public DhcpItem(Request request, DhcpOptions source, DhcpOptions target) {
        super(source.getDhcpOptionsId(), target.getDhcpOptionsId(),
                request, Type.DHCP, source, target);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public DhcpOptions getSource() {
        return super.getSource();
    }

    @Override
    public void setSource(DhcpOptions source) {
        super.setSource(source);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public DhcpOptions getTarget() {
        return super.getTarget();
    }

    @Override
    public void setTarget(DhcpOptions target) {
        super.setTarget(target);
    }

    public static class TypeConverter extends BaseItem.TypeConverter<DhcpOptions> {
        public TypeConverter() {
            super(DhcpOptions.class);
        }
    }
}
