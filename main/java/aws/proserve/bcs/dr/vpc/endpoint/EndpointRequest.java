// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.endpoint;

import aws.proserve.bcs.dr.vpc.rt.RouteTableRequest;

import java.util.Map;

public class EndpointRequest extends RouteTableRequest {
    private Map<String, String> securityGroupMap;
    private Map<String, String> routeTableMap;

    public Map<String, String> getSecurityGroupMap() {
        return securityGroupMap;
    }

    public void setSecurityGroupMap(Map<String, String> securityGroupMap) {
        this.securityGroupMap = securityGroupMap;
    }

    public Map<String, String> getRouteTableMap() {
        return routeTableMap;
    }

    public void setRouteTableMap(Map<String, String> routeTableMap) {
        this.routeTableMap = routeTableMap;
    }
}
