// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.vpc;

import aws.proserve.bcs.dr.lambda.StringHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.vpc.Cidr;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.util.Tagger;
import aws.proserve.bcs.dr.vpc.util.Zones;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AssociateVpcCidrBlockRequest;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.DescribeVpcAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.ModifyVpcAttributeRequest;
import com.amazonaws.services.ec2.model.VpcAttributeName;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.waiters.WaiterParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VpcWorker implements StringHandler<Request> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;
    private final DynamoDBMapper mapper;
    private final Tagger tagger;
    private final Zones zones;

    @Inject
    VpcWorker(
            @Source AmazonEC2 sourceEc2,
            @Target AmazonEC2 targetEc2,
            DynamoDBMapper mapper,
            Tagger tagger,
            Zones zones) {
        this.sourceEc2 = sourceEc2;
        this.targetEc2 = targetEc2;
        this.mapper = mapper;
        this.tagger = tagger;
        this.zones = zones;
    }

    @Override
    public String handleRequest(Request request, Context context) {
        final var sourceVpcId = request.getSource().getVpcId();
        log.info("Replicate VPC {}", request);

        final var vpcs = sourceEc2.describeVpcs(new DescribeVpcsRequest().withVpcIds(sourceVpcId)).getVpcs();
        if (vpcs.isEmpty()) {
            throw new IllegalArgumentException("Unable to find VPC with ID " + sourceVpcId);
        }

        final var vpc = vpcs.get(0);
        final String cidr;

        if (request.getCidr() == null || request.getCidr().isEmpty()) {
            cidr = vpc.getCidrBlock();
        } else {
            if (Cidr.matches(request.getCidr(), vpc.getCidrBlock())) {
                cidr = request.getCidr();
            } else {
                throw new IllegalArgumentException(
                        String.format("The two CIDRs do not match: %s, %s", vpc.getCidrBlock(), request.getCidr()));
            }
        }

        final var newVpc = targetEc2.createVpc(new CreateVpcRequest()
                .withCidrBlock(cidr)
                .withInstanceTenancy(vpc.getInstanceTenancy())
                .withAmazonProvidedIpv6CidrBlock(!vpc.getIpv6CidrBlockAssociationSet().isEmpty())).getVpc();
        targetEc2.waiters().vpcAvailable()
                .run(new WaiterParameters<>(new DescribeVpcsRequest().withVpcIds(newVpc.getVpcId())));

        final var support = sourceEc2.describeVpcAttribute(new DescribeVpcAttributeRequest()
                .withVpcId(sourceVpcId)
                .withAttribute(VpcAttributeName.EnableDnsSupport)).getEnableDnsSupport();
        final var hostnames = sourceEc2.describeVpcAttribute(new DescribeVpcAttributeRequest()
                .withVpcId(sourceVpcId)
                .withAttribute(VpcAttributeName.EnableDnsHostnames)).getEnableDnsHostnames();
        targetEc2.modifyVpcAttribute(new ModifyVpcAttributeRequest()
                .withVpcId(newVpc.getVpcId())
                .withEnableDnsSupport(support));
        targetEc2.modifyVpcAttribute(new ModifyVpcAttributeRequest()
                .withVpcId(newVpc.getVpcId())
                .withEnableDnsHostnames(hostnames));

        tagger.tag(newVpc.getVpcId(), vpc.getTags(),
                Tagger.sourceId(sourceVpcId),
                Tagger.sourceRegion(request.getSource().getRegion()));

        vpc.getCidrBlockAssociationSet().stream()
                // double association throws an exception
                .filter(i -> !i.getCidrBlock().equals(vpc.getCidrBlock()))
                .map(i -> new AssociateVpcCidrBlockRequest()
                        .withCidrBlock(i.getCidrBlock())
                        .withVpcId(newVpc.getVpcId()))
                .forEach(targetEc2::associateVpcCidrBlock);

        final var item = (VpcItem) new VpcItem(request, vpc, newVpc)
                .addProperty("zoneMap", zones.getZoneMap())
                .addProperty("cidr", request.getCidr())
                .addProperty("enableDnsSupport", support)
                .addProperty("enableDnsHostnames", hostnames);
        mapper.save(item);
        return newVpc.getVpcId();
    }
}
