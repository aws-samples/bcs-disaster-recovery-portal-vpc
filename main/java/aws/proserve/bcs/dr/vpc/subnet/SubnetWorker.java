// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.subnet;

import aws.proserve.bcs.dr.lambda.MapHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.vpc.Cidr;
import aws.proserve.bcs.dr.vpc.Filters;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.util.Tagger;
import aws.proserve.bcs.dr.vpc.util.Zones;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.ModifySubnetAttributeRequest;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.CreateDBSubnetGroupRequest;
import com.amazonaws.services.rds.model.DBSubnetGroup;
import com.amazonaws.services.rds.model.DescribeDBSubnetGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBSubnetGroupsResult;
import com.amazonaws.services.rds.model.Tag;
import com.amazonaws.waiters.WaiterParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class SubnetWorker implements MapHandler<Request> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;
    private final AmazonRDS sourceRds;
    private final AmazonRDS targetRds;
    private final DynamoDBMapper mapper;
    private final Tagger tagger;
    private final Zones zones;

    @Inject
    SubnetWorker(
            @Source AmazonEC2 sourceEc2,
            @Target AmazonEC2 targetEc2,
            @Source AmazonRDS sourceRds,
            @Target AmazonRDS targetRds,
            DynamoDBMapper mapper,
            Tagger tagger,
            Zones zones) {
        this.sourceEc2 = sourceEc2;
        this.targetEc2 = targetEc2;
        this.sourceRds = sourceRds;
        this.targetRds = targetRds;
        this.mapper = mapper;
        this.tagger = tagger;
        this.zones = zones;
    }

    @Override
    public Map<String, Object> handleRequest(Request request, Context context) {
        log.info("Replicate subnets {} ", request);

        final var response = new HashMap<String, Object>();
        final List<Subnet> subnets = new ArrayList<>();

        final var describeRequest = new DescribeSubnetsRequest()
                .withFilters(Filters.vpcId(request.getSource().getVpcId()));
        DescribeSubnetsResult result;
        do {
            result = sourceEc2.describeSubnets(describeRequest);
            subnets.addAll(result.getSubnets());

            describeRequest.setNextToken(result.getNextToken());
        } while (result.getNextToken() != null);

        if (subnets.isEmpty()) {
            log.info("This VPC contains zero subnets.");
            return response;
        }

        final var zoneMap = zones.getZoneMap();

        for (var subnet : subnets) {
            final String cidr;

            if (request.getCidr() == null || request.getCidr().isEmpty()) {
                cidr = subnet.getCidrBlock();
            } else {
                cidr = new Cidr(request.getCidr()).mask(new Cidr(subnet.getCidrBlock())).getBlock();
            }

            final var newSubnet = targetEc2.createSubnet(new CreateSubnetRequest()
                    .withVpcId(request.getTarget().getVpcId())
                    .withCidrBlock(cidr)
                    .withAvailabilityZone(zoneMap.get(subnet.getAvailabilityZone()))).getSubnet();
            targetEc2.waiters().subnetAvailable()
                    .run(new WaiterParameters<>(new DescribeSubnetsRequest()
                            .withSubnetIds(newSubnet.getSubnetId())));
            targetEc2.modifySubnetAttribute(new ModifySubnetAttributeRequest()
                    .withSubnetId(newSubnet.getSubnetId())
                    .withMapPublicIpOnLaunch(subnet.getMapPublicIpOnLaunch()));
            targetEc2.modifySubnetAttribute(new ModifySubnetAttributeRequest()
                    .withSubnetId(newSubnet.getSubnetId())
                    .withAssignIpv6AddressOnCreation(subnet.getAssignIpv6AddressOnCreation()));
            mapper.save(new SubnetItem(request, subnet, newSubnet));
            tagger.tag(newSubnet.getSubnetId(), subnet.getTags(),
                    Tagger.sourceId(subnet.getSubnetId()),
                    Tagger.sourceRegion(request.getSource().getRegion()),
                    Tagger.sourceZone(subnet.getAvailabilityZone()));
            response.put(subnet.getSubnetId(), newSubnet.getSubnetId());
        }

        replicateSubnetGroups(request, response);
        return response;
    }

    private void replicateSubnetGroups(Request request, Map<String, Object> subnetMap) {
        log.info("Replicate subnet groups {} ", request);

        final var groups = new ArrayList<DBSubnetGroup>();
        final var describeRequest = new DescribeDBSubnetGroupsRequest();
        DescribeDBSubnetGroupsResult result;
        do {
            result = sourceRds.describeDBSubnetGroups(describeRequest);
            describeRequest.setMarker(result.getMarker());

            for (var group : result.getDBSubnetGroups()) {
                final var subnetId = group.getSubnets().get(0).getSubnetIdentifier();
                final var vpcId = sourceEc2.describeSubnets(new DescribeSubnetsRequest().withSubnetIds(subnetId))
                        .getSubnets().get(0).getVpcId();
                if (request.getSource().getVpcId().equals(vpcId)) {
                    groups.add(group);
                }
            }
        } while (result.getMarker() != null);
        log.info("Found {} subnet groups of VPC {}", groups.size(), request.getSource().getVpcId());

        for (var group : groups) {
            targetRds.createDBSubnetGroup(new CreateDBSubnetGroupRequest()
                    .withDBSubnetGroupName(group.getDBSubnetGroupName())
                    .withDBSubnetGroupDescription(group.getDBSubnetGroupDescription())
                    .withSubnetIds(group.getSubnets().stream()
                            .map(com.amazonaws.services.rds.model.Subnet::getSubnetIdentifier)
                            .map(subnetMap::get)
                            .map(Object::toString)
                            .toArray(String[]::new))
                    .withTags(
                            new Tag().withKey("drp:source:id").withValue(group.getDBSubnetGroupName()),
                            new Tag().withKey("drp:source:region").withValue(request.getSource().getRegion())));
        }
    }
}
