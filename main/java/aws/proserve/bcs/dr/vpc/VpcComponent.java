// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc;


import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.vpc.dhcp.DhcpWorker;
import aws.proserve.bcs.dr.vpc.endpoint.EndpointWorker;
import aws.proserve.bcs.dr.vpc.igw.EgressIgwWorker;
import aws.proserve.bcs.dr.vpc.igw.IgwWorker;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.nacl.NetworkAclWorker;
import aws.proserve.bcs.dr.vpc.nat.NatGatewayWorker;
import aws.proserve.bcs.dr.vpc.rt.RouteTableWorker;
import aws.proserve.bcs.dr.vpc.sg.SecurityGroupRuleWorker;
import aws.proserve.bcs.dr.vpc.sg.SecurityGroupWorker;
import aws.proserve.bcs.dr.vpc.subnet.SubnetWorker;
import aws.proserve.bcs.dr.vpc.vpc.AddVpcItem;
import aws.proserve.bcs.dr.vpc.vpc.CheckVpcReplicated;
import aws.proserve.bcs.dr.vpc.vpc.CreateVpcProject;
import aws.proserve.bcs.dr.vpc.vpc.DeleteVpc;
import aws.proserve.bcs.dr.vpc.vpc.UpdateTargetVpc;
import aws.proserve.bcs.dr.vpc.vpc.VpcWorker;
import com.amazonaws.regions.Regions;
import dagger.BindsInstance;
import dagger.Component;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
@Component(modules = VpcModule.class)
public interface VpcComponent {

    static VpcComponent build(Request request) {
        return DaggerVpcComponent.builder()
                .sourceRegion(Regions.fromName(request.getSource().getRegion()))
                .targetRegion(Regions.fromName(request.getTarget().getRegion()))
                .build();
    }

    static VpcComponent build(Map<String, Object> request) {
        return DaggerVpcComponent.builder()
                .sourceRegion(Regions.fromName((String) request.get("sourceRegion")))
                .targetRegion(Regions.fromName((String) request.get("targetRegion")))
                .build();
    }

    AddVpcItem.Worker addItem();

    CheckVpcReplicated.Worker checkVpcReplicated();

    CreateVpcProject.Worker createProject();

    UpdateTargetVpc.Worker updateTargetVpc();

    DeleteVpc.Worker deleteVpc();

    DhcpWorker dhcpWorker();

    EndpointWorker endpointWorker();

    EgressIgwWorker egressIgwWorker();

    IgwWorker igwWorker();

    NatGatewayWorker natWorker();

    NetworkAclWorker networkAclWorker();

    RouteTableWorker routeTableWorker();

    SecurityGroupWorker securityGroupWorker();

    SecurityGroupRuleWorker securityGroupRuleWorker();

    SubnetWorker subnetWorker();

    VpcWorker vpcWorker();

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder sourceRegion(@Nullable @Source Regions source);

        @BindsInstance
        Builder targetRegion(@Nullable @Target Regions target);

        VpcComponent build();
    }
}
