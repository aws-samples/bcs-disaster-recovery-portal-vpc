// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.igw;

import aws.proserve.bcs.dr.lambda.MapHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.lambda.util.Assure;
import aws.proserve.bcs.dr.vpc.Filters;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.util.Tagger;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AttachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.CreateInternetGatewayRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class IgwWorker implements MapHandler<Request> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;
    private final DynamoDBMapper mapper;
    private final Tagger tagger;

    @Inject
    IgwWorker(
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
    public Map<String, Object> handleRequest(Request request, Context context) {
        log.info("Replicate internet gateway {}", request);

        final var describeRequest = new DescribeInternetGatewaysRequest()
                .withFilters(Filters.attachedVpcId(request.getSource().getVpcId()));
        final var gateways = sourceEc2.describeInternetGateways(describeRequest).getInternetGateways();

        if (gateways.isEmpty()) {
            log.info("This VPC contains zero internet gateways.");
            return null;
        }

        final var gateway = gateways.get(0);
        final var newGateway = targetEc2.createInternetGateway(new CreateInternetGatewayRequest()).getInternetGateway();
        Assure.assure(() -> targetEc2.describeInternetGateways(new DescribeInternetGatewaysRequest()
                .withInternetGatewayIds(newGateway.getInternetGatewayId())));
        targetEc2.attachInternetGateway(new AttachInternetGatewayRequest()
                .withVpcId(request.getTarget().getVpcId())
                .withInternetGatewayId(newGateway.getInternetGatewayId()));
        mapper.save(new IgwItem(request, gateway, newGateway));
        tagger.tag(newGateway.getInternetGatewayId(), gateway.getTags(),
                Tagger.sourceId(gateway.getInternetGatewayId()),
                Tagger.sourceRegion(request.getSource().getRegion()));

        return Map.of(gateway.getInternetGatewayId(), newGateway.getInternetGatewayId());
    }
}
