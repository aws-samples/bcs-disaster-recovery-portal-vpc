// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.igw;

import aws.proserve.bcs.dr.lambda.MapHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.lambda.util.Assure;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.util.Tagger;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateEgressOnlyInternetGatewayRequest;
import com.amazonaws.services.ec2.model.DescribeEgressOnlyInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeEgressOnlyInternetGatewaysResult;
import com.amazonaws.services.ec2.model.EgressOnlyInternetGateway;
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
public class EgressIgwWorker implements MapHandler<Request> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;
    private final DynamoDBMapper mapper;
    private final Tagger tagger;

    @Inject
    EgressIgwWorker(
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
        log.info("Replicate Egress IGW {}", request);

        final List<EgressOnlyInternetGateway> gateways = new ArrayList<>();
        final var describeRequest = new DescribeEgressOnlyInternetGatewaysRequest();
        DescribeEgressOnlyInternetGatewaysResult result;
        do {
            result = sourceEc2.describeEgressOnlyInternetGateways(describeRequest);
            gateways.addAll(result.getEgressOnlyInternetGateways());

            describeRequest.setNextToken(result.getNextToken());
        } while (result.getNextToken() != null);

        if (gateways.isEmpty()) {
            log.info("This VPC contains zero egress only internet gateways.");
            return null;
        }

        final var gatewayMap = new HashMap<String, Object>();
        for (var gateway : gateways) {
            for (var attachment : gateway.getAttachments()) {
                if (!attachment.getVpcId().equals(request.getSource().getVpcId())) {
                    continue;
                }

                final var newGateway = targetEc2.createEgressOnlyInternetGateway(new CreateEgressOnlyInternetGatewayRequest()
                        .withVpcId(request.getTarget().getVpcId())).getEgressOnlyInternetGateway();
                gatewayMap.put(gateway.getEgressOnlyInternetGatewayId(), newGateway.getEgressOnlyInternetGatewayId());
                Assure.assure(() -> targetEc2.describeEgressOnlyInternetGateways(new DescribeEgressOnlyInternetGatewaysRequest()
                        .withEgressOnlyInternetGatewayIds(newGateway.getEgressOnlyInternetGatewayId())));
                mapper.save(new EgressIgwItem(request, gateway, newGateway));
                tagger.tag(newGateway.getEgressOnlyInternetGatewayId(), gateway.getTags(),
                        Tagger.sourceId(gateway.getEgressOnlyInternetGatewayId()),
                        Tagger.sourceRegion(request.getSource().getRegion()));
            }
        }

        return gatewayMap;
    }
}
