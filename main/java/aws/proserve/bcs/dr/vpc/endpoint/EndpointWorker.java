// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.endpoint;

import aws.proserve.bcs.dr.lambda.StringHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.lambda.util.Assure;
import aws.proserve.bcs.dr.vpc.Filters;
import aws.proserve.bcs.dr.vpc.util.Tagger;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateVpcEndpointRequest;
import com.amazonaws.services.ec2.model.DescribeVpcEndpointsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcEndpointsResult;
import com.amazonaws.services.ec2.model.SecurityGroupIdentifier;
import com.amazonaws.services.ec2.model.VpcEndpoint;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class EndpointWorker implements StringHandler<EndpointRequest> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;
    private final DynamoDBMapper mapper;
    private final Tagger tagger;

    private EndpointRequest request;

    @Inject
    EndpointWorker(
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
    public String handleRequest(EndpointRequest request, Context context) {
        this.request = request;
        log.info("Replicate endpoint {}", request);

        final List<VpcEndpoint> endpoints = new ArrayList<>();

        final var describeRequest = new DescribeVpcEndpointsRequest()
                .withFilters(Filters.vpcId(request.getSource().getVpcId()));
        DescribeVpcEndpointsResult result;
        do {
            result = sourceEc2.describeVpcEndpoints(describeRequest);
            endpoints.addAll(result.getVpcEndpoints());

            describeRequest.setNextToken(result.getNextToken());
        } while (result.getNextToken() != null);

        if (endpoints.isEmpty()) {
            log.info("This VPC contains zero endpoints.");
            return request.getTarget().getVpcId();
        }

        for (var endpoint : endpoints) {
            final var newEndpoint = targetEc2.createVpcEndpoint(new CreateVpcEndpointRequest()
                    .withVpcId(request.getTarget().getVpcId())
                    .withVpcEndpointType(endpoint.getVpcEndpointType())
                    .withPrivateDnsEnabled(endpoint.isPrivateDnsEnabled())
                    .withServiceName(endpoint.getServiceName()
                            .replace(request.getSource().getRegion(), request.getTarget().getRegion()))
                    .withSubnetIds(endpoint.getSubnetIds().stream()
                            .map(id -> request.getSubnetMap().get(id))
                            .collect(Collectors.toList()))
                    .withSecurityGroupIds(endpoint.getGroups().stream()
                            .map(SecurityGroupIdentifier::getGroupId)
                            .map(id -> request.getSecurityGroupMap().get(id))
                            .collect(Collectors.toList()))
                    .withRouteTableIds(endpoint.getRouteTableIds().stream()
                            .map(id -> request.getRouteTableMap().get(id))
                            .collect(Collectors.toList()))).getVpcEndpoint();
            Assure.assure(() -> targetEc2.describeVpcEndpoints(new DescribeVpcEndpointsRequest()
                    .withVpcEndpointIds(newEndpoint.getVpcEndpointId())));
            mapper.save(new EndpointItem(request, endpoint, newEndpoint));
            tagger.tag(newEndpoint.getVpcEndpointId(), endpoint.getTags(),
                    Tagger.sourceId(endpoint.getVpcEndpointId()),
                    Tagger.sourceRegion(request.getSource().getRegion()));
        }

        return request.getTarget().getVpcId();
    }

}
