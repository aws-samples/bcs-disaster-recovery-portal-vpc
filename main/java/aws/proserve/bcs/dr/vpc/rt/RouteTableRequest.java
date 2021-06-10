// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.rt;

import aws.proserve.bcs.dr.vpc.model.Request;

import java.util.Map;

public class RouteTableRequest extends Request {
    private Map<String, String> subnetMap;
    private Map<String, String> egressGatewayMap;
    private Map<String, String> internetGatewayMap;
    private Map<String, String> natGatewayMap;

    public Map<String, String> getSubnetMap() {
        return subnetMap;
    }

    public void setSubnetMap(Map<String, String> subnetMap) {
        this.subnetMap = subnetMap;
    }

    public Map<String, String> getEgressGatewayMap() {
        return egressGatewayMap;
    }

    public void setEgressGatewayMap(Map<String, String> egressGatewayMap) {
        this.egressGatewayMap = egressGatewayMap;
    }

    public Map<String, String> getInternetGatewayMap() {
        return internetGatewayMap;
    }

    public void setInternetGatewayMap(Map<String, String> internetGatewayMap) {
        this.internetGatewayMap = internetGatewayMap;
    }

    public Map<String, String> getNatGatewayMap() {
        return natGatewayMap;
    }

    public void setNatGatewayMap(Map<String, String> natGatewayMap) {
        this.natGatewayMap = natGatewayMap;
    }
}
