// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.vpc;

import aws.proserve.bcs.dr.vpc.model.BaseItem;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.model.Type;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.ec2.model.Vpc;

public class VpcItem extends BaseItem<Vpc> {

    public VpcItem() {
    }

    public VpcItem(Request request, Vpc source, Vpc target) {
        super(source.getVpcId(), target.getVpcId(), request, Type.VPC, source, target);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public Vpc getSource() {
        return super.getSource();
    }

    /**
     * @apiNote DynamoDB needs this reified setter to properly unconvert map to object.
     */
    @Override
    public void setSource(Vpc source) {
        super.setSource(source);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public Vpc getTarget() {
        return super.getTarget();
    }

    @Override
    public void setTarget(Vpc target) {
        super.setTarget(target);
    }

    public static class TypeConverter extends BaseItem.TypeConverter<Vpc> {
        public TypeConverter() {
            super(Vpc.class);
        }
    }
}
