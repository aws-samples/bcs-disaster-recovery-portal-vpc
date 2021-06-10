// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.nacl;

import aws.proserve.bcs.dr.lambda.VoidHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.lambda.util.Assure;
import aws.proserve.bcs.dr.vpc.Filters;
import aws.proserve.bcs.dr.vpc.model.SubnetRequest;
import aws.proserve.bcs.dr.vpc.util.Tagger;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateNetworkAclEntryRequest;
import com.amazonaws.services.ec2.model.CreateNetworkAclRequest;
import com.amazonaws.services.ec2.model.DescribeNetworkAclsRequest;
import com.amazonaws.services.ec2.model.DescribeNetworkAclsResult;
import com.amazonaws.services.ec2.model.NetworkAcl;
import com.amazonaws.services.ec2.model.NetworkAclAssociation;
import com.amazonaws.services.ec2.model.ReplaceNetworkAclAssociationRequest;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class NetworkAclWorker implements VoidHandler<SubnetRequest> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;
    private final DynamoDBMapper mapper;
    private final Tagger tagger;

    @Inject
    NetworkAclWorker(
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
    public void handleRequest(SubnetRequest request, Context context) {
        log.info("Replicate network ACL {}", request);

        final List<NetworkAcl> acls = new ArrayList<>();
        final var describeRequest = new DescribeNetworkAclsRequest()
                .withFilters(Filters.vpcId(request.getSource().getVpcId()));
        DescribeNetworkAclsResult result;
        do {
            result = sourceEc2.describeNetworkAcls(describeRequest);
            acls.addAll(result.getNetworkAcls());

            describeRequest.setNextToken(result.getNextToken());
        } while (result.getNextToken() != null);

        if (acls.isEmpty()) {
            log.info("This VPC contains zero network ACLs.");
            return;
        }

        for (var acl : acls) {
            final var newAcl = targetEc2.createNetworkAcl(
                    new CreateNetworkAclRequest().withVpcId(request.getTarget().getVpcId())).getNetworkAcl();
            Assure.assure(() -> targetEc2.describeNetworkAcls(new DescribeNetworkAclsRequest()
                    .withNetworkAclIds(newAcl.getNetworkAclId())));

            for (var association : acl.getAssociations()) {
                final var targetSubnetId = request.getSubnetMap().get(association.getSubnetId());

                final var assoId = targetEc2.describeNetworkAcls(new DescribeNetworkAclsRequest()
                        .withFilters(Filters.associatedSubnetId(targetSubnetId)))
                        .getNetworkAcls().get(0)
                        .getAssociations().stream()
                        .filter(asso -> asso.getSubnetId().equals(targetSubnetId))
                        .findFirst()
                        .map(NetworkAclAssociation::getNetworkAclAssociationId)
                        .orElse(null);

                if (assoId == null) {
                    log.warn("Unable to find an association ID for target subnet [{}]", targetSubnetId);
                } else {
                    log.info("Associate subnet {} with new NAcl {}", targetSubnetId, newAcl.getNetworkAclId());
                    targetEc2.replaceNetworkAclAssociation(new ReplaceNetworkAclAssociationRequest()
                            .withAssociationId(assoId)
                            .withNetworkAclId(newAcl.getNetworkAclId()));
                }
            }

            for (var entry : acl.getEntries()) {
                if (entry.getRuleNumber() > 32766) {
                    continue;
                }

                targetEc2.createNetworkAclEntry(new CreateNetworkAclEntryRequest()
                        .withNetworkAclId(newAcl.getNetworkAclId())
                        .withCidrBlock(entry.getCidrBlock())
                        .withEgress(entry.getEgress())
                        .withIcmpTypeCode(entry.getIcmpTypeCode())
                        .withIpv6CidrBlock(entry.getIpv6CidrBlock())
                        .withPortRange(entry.getPortRange())
                        .withProtocol(entry.getProtocol())
                        .withRuleAction(entry.getRuleAction())
                        .withRuleNumber(entry.getRuleNumber()));
            }
            Assure.assure(() -> targetEc2.describeNetworkAcls(new DescribeNetworkAclsRequest()
                    .withNetworkAclIds(newAcl.getNetworkAclId())));
            mapper.save(new NetworkAclItem(request, acl, newAcl));
            tagger.tag(newAcl.getNetworkAclId(), acl.getTags(),
                    Tagger.sourceId(acl.getNetworkAclId()),
                    Tagger.sourceRegion(request.getSource().getRegion()));
        }
    }
}
