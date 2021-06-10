// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.vpc;

import aws.proserve.bcs.dr.lambda.StringHandler;
import aws.proserve.bcs.dr.project.ProjectFinder;
import aws.proserve.bcs.dr.vpc.DaggerVpcComponent;
import com.amazonaws.services.lambda.runtime.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

public class UpdateTargetVpc implements StringHandler<UpdateTargetVpc.Request> {

    @Override
    public String handleRequest(UpdateTargetVpc.Request request, Context context) {
        return DaggerVpcComponent.builder().build().updateTargetVpc().update(request);
    }

    @Singleton
    public static class Worker {
        private final ProjectFinder finder;

        @Inject
        Worker(ProjectFinder finder) {
            this.finder = finder;
        }

        private String update(UpdateTargetVpc.Request request) {
            final var project = finder.findOne(request.getProjectId());
            final var vpcItems = project.getVpcProject().getItems();
            // It may receive output from step functions directly, which is a JSON string.
            final var targetVpcId = request.getTargetVpcId().replaceAll("\"", "");

            vpcItems.stream().filter(i -> i.getId().equals(request.getItemId()))
                    .findFirst()
                    .ifPresent(i -> i.setTarget(targetVpcId));

            finder.save(project);
            return targetVpcId;
        }
    }

    static final class Request {
        private String projectId;
        private String itemId;
        private String targetVpcId;

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getTargetVpcId() {
            return targetVpcId;
        }

        public void setTargetVpcId(String targetVpcId) {
            this.targetVpcId = targetVpcId;
        }
    }
}
