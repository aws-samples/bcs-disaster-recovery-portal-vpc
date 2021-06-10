// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.rt;

import aws.proserve.bcs.dr.lambda.MapHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.lambda.util.Assure;
import aws.proserve.bcs.dr.vpc.Filters;
import aws.proserve.bcs.dr.vpc.util.Tagger;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AssociateRouteTableRequest;
import com.amazonaws.services.ec2.model.CreateRouteRequest;
import com.amazonaws.services.ec2.model.CreateRouteTableRequest;
import com.amazonaws.services.ec2.model.DescribeEgressOnlyInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeNatGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.ReplaceRouteTableAssociationRequest;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RouteTableAssociation;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class RouteTableWorker implements MapHandler<RouteTableRequest> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;
    private final DynamoDBMapper mapper;
    private final Tagger tagger;

    private RouteTableRequest request;

    @Inject
    RouteTableWorker(
            @Source AmazonEC2 sourceEc2,
            @Target AmazonEC2 targetEc2,
            DynamoDBMapper mapper,
            Tagger tagger) {
        this.sourceEc2 = sourceEc2;
        this.targetEc2 = targetEc2;
        this.mapper = mapper;
        this.tagger = tagger;
    }

    @Override
    public Map<String, Object> handleRequest(RouteTableRequest request, Context context) {
        this.request = request;
        log.info("Replicate route table {}", request);

        final var response = new HashMap<String, Object>();
        final var tables = findAllTables(request.getSource().getVpcId(), sourceEc2);
        if (tables.isEmpty()) {
            log.info("This VPC contains zero route tables.");
            return response;
        }

        for (var table : tables) {
            final var newTable = targetEc2.createRouteTable(new CreateRouteTableRequest()
                    .withVpcId(request.getTarget().getVpcId())).getRouteTable();
            Assure.assure(() -> targetEc2.describeRouteTables(new DescribeRouteTablesRequest()
                    .withRouteTableIds(newTable.getRouteTableId())));
            response.put(table.getRouteTableId(), newTable.getRouteTableId());
            mapper.save(new RouteTableItem(request, table, newTable));
            // somehow describing is fine but tagging fails, saying route table does not exists
            Assure.assure(() -> tagger.tag(newTable.getRouteTableId(), table.getTags(),
                    Tagger.sourceId(table.getRouteTableId()),
                    Tagger.sourceRegion(request.getSource().getRegion())));

            for (var association : table.getAssociations()) {
                if (association.isMain()) {
                    final var targetAsso = findTargetMainAssociation();
                    if (targetAsso == null) {
                        log.warn("Unable to find the main association in target VPC {}", request.getTarget().getVpcId());
                    } else {
                        targetEc2.replaceRouteTableAssociation(new ReplaceRouteTableAssociationRequest()
                                .withRouteTableId(newTable.getRouteTableId())
                                .withAssociationId(targetAsso));
                    }
                }

                if (association.getSubnetId() != null) {
                    targetEc2.associateRouteTable(new AssociateRouteTableRequest()
                            .withRouteTableId(newTable.getRouteTableId())
                            .withSubnetId(request.getSubnetMap().get(association.getSubnetId())));
                }
            }

            for (var rule : table.getRoutes()) {
                if (rule.getLocalGatewayId() != null
                        || rule.getTransitGatewayId() != null
                        || rule.getInstanceId() != null
                        || rule.getVpcPeeringConnectionId() != null
                        || rule.getNetworkInterfaceId() != null
                        || rule.getDestinationPrefixListId() != null) {
                    log.warn("Skip the rule [{}] of table [{}] where its local and transit gateway, instance ID, " +
                                    "vpc peering, network interface and destination prefix list is non-null.",
                            rule, table.getRouteTableId());
                    continue;
                }

                if ((rule.getGatewayId() == null || rule.getGatewayId().equals("local"))
                        && rule.getNatGatewayId() == null
                        && rule.getEgressOnlyInternetGatewayId() == null) {
                    log.warn("Skip the rule [{}] of table [{}] as its gateway ID is null or local and " +
                            "both of its NAT gateway and egress only IGW are null.", rule, table.getRouteTableId());
                    continue;
                }

                final String id;
                final var createRequest = new CreateRouteRequest()
                        .withRouteTableId(newTable.getRouteTableId())
                        .withDestinationCidrBlock(rule.getDestinationCidrBlock())
                        .withDestinationIpv6CidrBlock(rule.getDestinationIpv6CidrBlock());
                if (rule.getGatewayId() != null) {
                    createRequest.withGatewayId(id = request.getInternetGatewayMap().get(rule.getGatewayId()));
                    Assure.assure(() -> targetEc2.describeInternetGateways(new DescribeInternetGatewaysRequest()
                            .withInternetGatewayIds(id)));
                } else if (rule.getNatGatewayId() != null) {
                    createRequest.withNatGatewayId(id = request.getNatGatewayMap().get(rule.getNatGatewayId()));
                    Assure.assure(() -> targetEc2.describeNatGateways(new DescribeNatGatewaysRequest()
                            .withNatGatewayIds(id)));
                } else if (rule.getEgressOnlyInternetGatewayId() != null) {
                    createRequest.withEgressOnlyInternetGatewayId(
                            id = request.getEgressGatewayMap().get(rule.getEgressOnlyInternetGatewayId()));
                    Assure.assure(() -> targetEc2.describeEgressOnlyInternetGateways(
                            new DescribeEgressOnlyInternetGatewaysRequest().withEgressOnlyInternetGatewayIds(id)));
                }
                final var hasGateway = createRequest.getGatewayId() != null
                        || createRequest.getNatGatewayId() != null
                        || createRequest.getEgressOnlyInternetGatewayId() != null;
                if (hasGateway) {
                    log.debug("Create route {}", createRequest);
                    targetEc2.createRoute(createRequest);
                } else {
                    log.warn("Skip the rule [{}] of table [{}] as it does not have any gateway attachment.",
                            rule, table.getRouteTableId());
                }
            }
        }

        return response;
    }

    private String findTargetMainAssociation() {
        return findAllTables(request.getTarget().getVpcId(), targetEc2)
                .stream()
                .filter(t -> t.getAssociations().stream().anyMatch(RouteTableAssociation::isMain))
                .flatMap(t -> t.getAssociations().stream())
                .filter(RouteTableAssociation::isMain)
                .map(RouteTableAssociation::getRouteTableAssociationId)
                .findFirst().orElse(null);
    }

    private List<RouteTable> findAllTables(String vpcId, AmazonEC2 ec2) {
        final List<RouteTable> tables = new ArrayList<>();
        final var describeRequest = new DescribeRouteTablesRequest()
                .withFilters(Filters.vpcId(vpcId));
        DescribeRouteTablesResult result;
        do {
            result = ec2.describeRouteTables(describeRequest);
            tables.addAll(result.getRouteTables());

            describeRequest.setNextToken(result.getNextToken());
        } while (result.getNextToken() != null);

        return tables;
    }
}
