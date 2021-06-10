// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.dhcp;

import aws.proserve.bcs.dr.lambda.StringHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.lambda.util.Assure;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.util.Tagger;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AssociateDhcpOptionsRequest;
import com.amazonaws.services.ec2.model.CreateDhcpOptionsRequest;
import com.amazonaws.services.ec2.model.DescribeDhcpOptionsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DhcpWorker implements StringHandler<Request> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;
    private final DynamoDBMapper mapper;
    private final Tagger tagger;

    @Inject
    DhcpWorker(
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
    public String handleRequest(Request request, Context context) {
        log.info("Replicate DHCP {}", request);

        final var sourceVpc = sourceEc2.describeVpcs(
                new DescribeVpcsRequest().withVpcIds(request.getSource().getVpcId())).getVpcs().get(0);
        final var sourceDhcps = sourceEc2.describeDhcpOptions(
                new DescribeDhcpOptionsRequest().withDhcpOptionsIds(sourceVpc.getDhcpOptionsId())).getDhcpOptions();

        if (sourceDhcps.isEmpty()) {
            log.info("Source VPC has zero DHCP.");
            return null;
        }

        final var sourceDhcp = sourceDhcps.get(0);
        final var targetDhcp = targetEc2.createDhcpOptions(
                new CreateDhcpOptionsRequest(sourceDhcp.getDhcpConfigurations())).getDhcpOptions();
        Assure.assure(() -> targetEc2.describeDhcpOptions(new DescribeDhcpOptionsRequest()
                .withDhcpOptionsIds(targetDhcp.getDhcpOptionsId())));
        mapper.save(new DhcpItem(request, sourceDhcp, targetDhcp));
        tagger.tag(targetDhcp.getDhcpOptionsId(), sourceDhcp.getTags(),
                Tagger.sourceId(sourceDhcp.getDhcpOptionsId()),
                Tagger.sourceRegion(request.getSource().getRegion()));

        targetEc2.associateDhcpOptions(new AssociateDhcpOptionsRequest()
                .withVpcId(request.getTarget().getVpcId())
                .withDhcpOptionsId(targetDhcp.getDhcpOptionsId()));

        return targetDhcp.getDhcpOptionsId();
    }
}
