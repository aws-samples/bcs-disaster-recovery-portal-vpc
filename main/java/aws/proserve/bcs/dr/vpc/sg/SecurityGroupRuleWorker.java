// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.sg;

import aws.proserve.bcs.dr.lambda.VoidHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.vpc.util.Tagger;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupEgressRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupEgressRequest;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Singleton
public class SecurityGroupRuleWorker implements VoidHandler<SecurityGroupRuleRequest> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;
    private final DynamoDBMapper mapper;
    private final Tagger tagger;

    @Inject
    SecurityGroupRuleWorker(
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
    public void handleRequest(SecurityGroupRuleRequest request, Context context) {
        log.info("Replicate security group rules {}", request);

        final var groupMap = request.getSecurityGroupMap();
        for (var entry : groupMap.entrySet()) {
            final var sourceGroup = sourceEc2.describeSecurityGroups(
                    new DescribeSecurityGroupsRequest().withGroupIds(entry.getKey())).getSecurityGroups().get(0);
            final var targetGroup = targetEc2.describeSecurityGroups(
                    new DescribeSecurityGroupsRequest().withGroupIds(entry.getValue())).getSecurityGroups().get(0);
            log.info("Replicate {} to {}", entry.getKey(), entry.getValue());

            final var newIns = new ArrayList<>(sourceGroup.getIpPermissions());
            newIns.removeIf(o -> !o.getPrefixListIds().isEmpty());
            for (var in : newIns) {
                final var pairs = new ArrayList<>(in.getUserIdGroupPairs());
                pairs.removeIf(p -> p.getVpcPeeringConnectionId() != null);
                in.setUserIdGroupPairs(pairs.stream()
                        .map(p -> p.withGroupId(groupMap.get(p.getGroupId())))
                        .collect(Collectors.toList()));
            }

            if (!targetGroup.getIpPermissions().isEmpty()) {
                targetEc2.revokeSecurityGroupEgress(new RevokeSecurityGroupEgressRequest()
                        .withGroupId(targetGroup.getGroupId())
                        .withIpPermissions(targetGroup.getIpPermissions()));
            }
            if (newIns.isEmpty()) {
                log.info("No inbound rule for security group {}", sourceGroup.getGroupId());
            } else {
                targetEc2.authorizeSecurityGroupIngress(new AuthorizeSecurityGroupIngressRequest()
                        .withGroupId(targetGroup.getGroupId())
                        .withIpPermissions(newIns));
            }

            final var newOuts = new ArrayList<>(sourceGroup.getIpPermissionsEgress());
            newOuts.removeIf(o -> !o.getPrefixListIds().isEmpty());
            for (var out : newOuts) {
                final var pairs = new ArrayList<>(out.getUserIdGroupPairs());
                pairs.removeIf(p -> p.getVpcPeeringConnectionId() != null);
                out.setUserIdGroupPairs(pairs.stream()
                        .map(p -> p.withGroupId(groupMap.get(p.getGroupId())))
                        .collect(Collectors.toList()));
            }

            if (!targetGroup.getIpPermissionsEgress().isEmpty()) {
                targetEc2.revokeSecurityGroupEgress(new RevokeSecurityGroupEgressRequest()
                        .withGroupId(targetGroup.getGroupId())
                        .withIpPermissions(targetGroup.getIpPermissionsEgress()));
            }
            if (newOuts.isEmpty()) {
                log.info("No outbound rule for security group {}", sourceGroup.getGroupId());
            } else {
                targetEc2.authorizeSecurityGroupEgress(new AuthorizeSecurityGroupEgressRequest()
                        .withGroupId(targetGroup.getGroupId())
                        .withIpPermissions(newOuts));
            }
        }
    }
}
