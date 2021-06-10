// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.model;

import aws.proserve.bcs.dr.project.Side;

public class Request {
    private Side side;
    private VpcInfo source;
    private VpcInfo target;
    private String cidr;
    private boolean continuous;

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public VpcInfo getSource() {
        return source;
    }

    public void setSource(VpcInfo source) {
        this.source = source;
    }

    public VpcInfo getTarget() {
        return target;
    }

    public void setTarget(VpcInfo target) {
        this.target = target;
    }

    public VpcInfo getVpcInfo(Side side) {
        return side == Side.source ? getSource() : getTarget();
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public void setIsContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    @Override
    public String toString() {
        return "Request{" +
                "source=" + source +
                ", target=" + target +
                ", cidr='" + cidr + '\'' +
                ", continuous=" + continuous +
                '}';
    }
}
