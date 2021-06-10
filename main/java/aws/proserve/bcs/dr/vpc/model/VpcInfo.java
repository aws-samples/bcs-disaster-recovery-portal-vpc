// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.model;

public class VpcInfo {
    private String vpcId;
    private String region;

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public String toString() {
        return "VpcInfo{" +
                "vpcId='" + vpcId + '\'' +
                ", region='" + region + '\'' +
                '}';
    }
}
