// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.nacl;

import aws.proserve.bcs.dr.vpc.model.BaseItem;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.model.Type;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.ec2.model.NetworkAcl;

public class NetworkAclItem extends BaseItem<NetworkAcl> {

    public NetworkAclItem() {
    }

    public NetworkAclItem(Request request, NetworkAcl source, NetworkAcl target) {
        super(source.getNetworkAclId(), target.getNetworkAclId(),
                request, Type.NETWORK_ACL, source, target);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public NetworkAcl getSource() {
        return super.getSource();
    }

    @Override
    public void setSource(NetworkAcl source) {
        super.setSource(source);
    }

    @Override
    @DynamoDBTypeConverted(converter = TypeConverter.class)
    public NetworkAcl getTarget() {
        return super.getTarget();
    }

    @Override
    public void setTarget(NetworkAcl target) {
        super.setTarget(target);
    }

    public static class TypeConverter extends BaseItem.TypeConverter<NetworkAcl> {
        public TypeConverter() {
            super(NetworkAcl.class);
        }
    }
}
