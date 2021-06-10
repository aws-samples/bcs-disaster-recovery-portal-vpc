// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.sg;

import aws.proserve.bcs.dr.vpc.model.Request;

import java.util.Map;

class SecurityGroupRuleRequest extends Request {
    private Map<String, String> securityGroupMap;

    public Map<String, String> getSecurityGroupMap() {
        return securityGroupMap;
    }

    public void setSecurityGroupMap(Map<String, String> securityGroupMap) {
        this.securityGroupMap = securityGroupMap;
    }
}
