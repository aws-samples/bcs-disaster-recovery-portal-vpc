// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.rt;

import aws.proserve.bcs.dr.vpc.model.BaseItem;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.model.Type;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.ec2.model.RouteTable;

public class RouteTableItem extends BaseItem<RouteTable> {

    public RouteTableItem() {
    }

    public RouteTableItem(Request request, RouteTable source, RouteTable target) {
        super(source.getRouteTableId(), target.getRouteTableId(),
                request, Type.ROUTE_TABLE, source, target);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public RouteTable getSource() {
        return super.getSource();
    }

    @Override
    public void setSource(RouteTable source) {
        super.setSource(source);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public RouteTable getTarget() {
        return super.getTarget();
    }

    @Override
    public void setTarget(RouteTable target) {
        super.setTarget(target);
    }

    public static class TypeConverter extends BaseItem.TypeConverter<RouteTable> {
        public TypeConverter() {
            super(RouteTable.class);
        }
    }
}
