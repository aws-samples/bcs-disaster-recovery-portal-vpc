// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

public enum Type {
    VPC,
    DHCP,
    EGRESS_GATEWAY,
    ENDPOINT,
    INTERNET_GATEWAY,
    NAT_GATEWAY,
    NETWORK_ACL,
    ROUTE_TABLE,
    SECURITY_GROUP,
    SUBNET;

    public static class Converter implements DynamoDBTypeConverter<String, Type> {
        @Override
        public String convert(Type object) {
            return object.name();
        }

        @Override
        public Type unconvert(String object) {
            return Type.valueOf(object);
        }
    }
}
