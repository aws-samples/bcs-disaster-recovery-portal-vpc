// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.endpoint;

import aws.proserve.bcs.dr.vpc.model.BaseItem;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.model.Type;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.ec2.model.VpcEndpoint;

public class EndpointItem extends BaseItem<VpcEndpoint> {

    public EndpointItem() {
    }

    public EndpointItem(Request request, VpcEndpoint source, VpcEndpoint target) {
        super(source.getVpcEndpointId(), target.getVpcEndpointId(),
                request, Type.ENDPOINT, source, target);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public VpcEndpoint getSource() {
        return super.getSource();
    }

    @Override
    public void setSource(VpcEndpoint source) {
        super.setSource(source);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public VpcEndpoint getTarget() {
        return super.getTarget();
    }

    @Override
    public void setTarget(VpcEndpoint target) {
        super.setTarget(target);
    }

    public static class TypeConverter extends BaseItem.TypeConverter<VpcEndpoint> {
        public TypeConverter() {
            super(VpcEndpoint.class);
        }
    }
}
