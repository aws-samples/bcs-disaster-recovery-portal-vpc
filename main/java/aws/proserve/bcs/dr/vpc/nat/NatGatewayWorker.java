// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.nat;

import aws.proserve.bcs.dr.lambda.MapHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.lambda.util.Assure;
import aws.proserve.bcs.dr.vpc.Filters;
import aws.proserve.bcs.dr.vpc.model.SubnetRequest;
import aws.proserve.bcs.dr.vpc.util.Tagger;
import aws.proserve.bcs.dr.vpc.util.Zones;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.CreateNatGatewayRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeNatGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeNatGatewaysResult;
import com.amazonaws.services.ec2.model.DomainType;
import com.amazonaws.services.ec2.model.NatGateway;
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
public class NatGatewayWorker implements MapHandler<SubnetRequest> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;
    private final DynamoDBMapper mapper;
    private final Tagger tagger;
    private final Zones zones;

    @Inject
    NatGatewayWorker(
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
    public Map<String, Object> handleRequest(SubnetRequest request, Context context) {
        log.info("Replicate NAT gateways {}", request);

        final List<NatGateway> nats = new ArrayList<>();
        final var response = new HashMap<String, Object>();
        final var describeRequest = new DescribeNatGatewaysRequest()
                .withFilter(Filters.vpcId(request.getSource().getVpcId()));

        DescribeNatGatewaysResult result;
        do {
            result = sourceEc2.describeNatGateways(describeRequest);
            nats.addAll(result.getNatGateways());

            describeRequest.setNextToken(result.getNextToken());
        } while (result.getNextToken() != null);

        if (nats.isEmpty()) {
            log.info("This VPC contains zero NAT gateways.");
            return response;
        }

        for (var nat : nats) {
            final var targetSubnetId = request.getSubnetMap().get(nat.getSubnetId());
            final var allocation = targetEc2.allocateAddress(new AllocateAddressRequest().withDomain(DomainType.Vpc));
            Assure.assure(() -> targetEc2.describeAddresses(new DescribeAddressesRequest()
                    .withAllocationIds(allocation.getAllocationId())));
            tagger.tag(allocation.getAllocationId());

            final var newNat = targetEc2.createNatGateway(new CreateNatGatewayRequest()
                    .withAllocationId(allocation.getAllocationId())
                    .withSubnetId(targetSubnetId)).getNatGateway();
            response.put(nat.getNatGatewayId(), newNat.getNatGatewayId());
            Assure.assure(() -> targetEc2.describeNatGateways(new DescribeNatGatewaysRequest()
                    .withNatGatewayIds(newNat.getNatGatewayId())));
            mapper.save(new NatGatewayItem(request, nat, newNat));
            tagger.tag(newNat.getNatGatewayId(), nat.getTags(),
                    Tagger.sourceId(nat.getNatGatewayId()),
                    Tagger.sourceRegion(request.getSource().getRegion()));
        }

        return response;
    }
}
