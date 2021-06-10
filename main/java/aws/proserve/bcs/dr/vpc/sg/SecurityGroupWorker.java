// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.sg;

import aws.proserve.bcs.dr.lambda.MapHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.vpc.Filters;
import aws.proserve.bcs.dr.vpc.model.Request;
import aws.proserve.bcs.dr.vpc.util.Tagger;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.waiters.WaiterParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
public class SecurityGroupWorker implements MapHandler<Request> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;
    private final DynamoDBMapper mapper;
    private final Tagger tagger;

    @Inject
    SecurityGroupWorker(
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
        log.info("Replicate security groups {}", request);

        final var response = new HashMap<String, Object>();
        final List<SecurityGroup> groups = new ArrayList<>();

        final var describeRequest = new DescribeSecurityGroupsRequest()
                .withFilters(Filters.vpcId(request.getSource().getVpcId()));
        DescribeSecurityGroupsResult result;
        do {
            result = sourceEc2.describeSecurityGroups(describeRequest);
            groups.addAll(result.getSecurityGroups());

            describeRequest.setNextToken(result.getNextToken());
        } while (result.getNextToken() != null);

        if (groups.isEmpty()) {
            log.info("This VPC contains zero security groups.");
            return response;
        }

        for (var group : groups) {
            final var groupName = group.getGroupName().equals("default") ?
                    "source-default-" + UUID.randomUUID().toString() : group.getGroupName();
            final var newGroup = targetEc2.createSecurityGroup(new CreateSecurityGroupRequest()
                    .withGroupName(groupName)
                    .withVpcId(request.getTarget().getVpcId())
                    .withDescription(group.getDescription()));
            response.put(group.getGroupId(), newGroup.getGroupId());

            // Sometimes, newly created is not found (don't know why), thus tagging failed.
            targetEc2.waiters().securityGroupExists()
                    .run(new WaiterParameters<>(new DescribeSecurityGroupsRequest()
                            .withGroupIds(newGroup.getGroupId())));
            mapper.save(new SecurityGroupItem(request, group, targetEc2.describeSecurityGroups(
                    new DescribeSecurityGroupsRequest().withGroupIds(newGroup.getGroupId())).getSecurityGroups().get(0)));
            tagger.tag(newGroup.getGroupId(), group.getTags(),
                    Tagger.sourceId(group.getGroupId()),
                    Tagger.sourceRegion(request.getSource().getRegion()));
        }

        return response;
    }
}
