// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.igw;

import aws.proserve.bcs.dr.vpc.model.BaseItem;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.model.Type;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.ec2.model.InternetGateway;

public class IgwItem extends BaseItem<InternetGateway> {

    public IgwItem() {
    }

    public IgwItem(Request request, InternetGateway source, InternetGateway target) {
        super(source.getInternetGatewayId(), target.getInternetGatewayId(),
                request, Type.INTERNET_GATEWAY, source, target);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public InternetGateway getSource() {
        return super.getSource();
    }

    @Override
    public void setSource(InternetGateway source) {
        super.setSource(source);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public InternetGateway getTarget() {
        return super.getTarget();
    }

    @Override
    public void setTarget(InternetGateway target) {
        super.setTarget(target);
    }

    public static class TypeConverter extends BaseItem.TypeConverter<InternetGateway> {
        public TypeConverter() {
            super(InternetGateway.class);
        }
    }
}
