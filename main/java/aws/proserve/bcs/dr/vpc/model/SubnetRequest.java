// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.model;

import java.util.Map;

public class SubnetRequest extends Request {
    private Map<String, String> subnetMap;

    public Map<String, String> getSubnetMap() {
        return subnetMap;
    }

    public void setSubnetMap(Map<String, String> subnetMap) {
        this.subnetMap = subnetMap;
    }
}
