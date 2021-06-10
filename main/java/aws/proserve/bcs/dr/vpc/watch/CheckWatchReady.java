// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.watch;

import aws.proserve.bcs.dr.lambda.BoolHandler;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClientBuilder;
import com.amazonaws.services.cloudwatchevents.model.ListRulesRequest;
import com.amazonaws.services.cloudwatchevents.model.ListRulesResult;
import com.amazonaws.services.cloudwatchevents.model.Rule;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.ListFunctionsRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.amazonaws.services.lambda.runtime.Context;

public class CheckWatchReady implements BoolHandler<String> {

    private String region;

    @Override
    public boolean handleRequest(String region, Context context) {
        this.region = region;
        return isFunctionReady() && isRuleReady();
    }

    private boolean isFunctionReady() {
        final var lambda = AWSLambdaClientBuilder.standard().withRegion(region).build();
        final var request = new ListFunctionsRequest();
        ListFunctionsResult result;
        do {
            result = lambda.listFunctions(request);
            request.setMarker(result.getNextMarker());

            if (result.getFunctions().stream()
                    .map(FunctionConfiguration::getFunctionName)
                    .anyMatch(name -> name.contains("DRPVpcReplicateWatch"))) {
                return true;
            }
        } while (result.getNextMarker() != null);
        return false;
    }

    private boolean isRuleReady() {
        final var watch = AmazonCloudWatchEventsClientBuilder.standard().withRegion(region).build();
        final var request = new ListRulesRequest();
        ListRulesResult result;
        do {
            result = watch.listRules(request);
            request.setNextToken(result.getNextToken());

            if (result.getRules().stream()
                    .map(Rule::getName)
                    .anyMatch(name -> name.contains("VpcWatchRule"))) {
                return true;
            }
        } while (result.getNextToken() != null);
        return false;
    }
}
