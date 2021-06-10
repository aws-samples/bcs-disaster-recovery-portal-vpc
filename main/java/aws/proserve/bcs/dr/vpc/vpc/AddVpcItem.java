// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.vpc;

import aws.proserve.bcs.dr.lambda.BoolHandler;
import aws.proserve.bcs.dr.project.ProjectFinder;
import aws.proserve.bcs.dr.project.States;
import aws.proserve.bcs.dr.vpc.DaggerVpcComponent;
import aws.proserve.bcs.dr.vpc.VpcItem;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

public class AddVpcItem implements BoolHandler<AddVpcItem.Request> {

    @Override
    public boolean handleRequest(AddVpcItem.Request request, Context context) {
        return DaggerVpcComponent.builder().build().addItem().add(request);
    }

    @Singleton
    public static class Worker {
        private final Logger log = LoggerFactory.getLogger(getClass());
        private final ProjectFinder finder;

        @Inject
        Worker(ProjectFinder finder) {
            this.finder = finder;
        }

        private boolean add(AddVpcItem.Request request) {
            final var project = finder.findOne(request.getProjectId());
            final var item = request.getItem();

            final var vpcItems = project.getVpcProject().getItems();
            if (vpcItems.stream().map(VpcItem::getId).anyMatch(item.getId()::equals)) {
                log.warn("Skip duplicated item {}", item);
                return false;
            }

            item.setState(States.NEW);
            vpcItems.add(item);
            finder.save(project);
            return true;
        }
    }

    static final class Request {
        private String projectId;
        private VpcItem item;

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public VpcItem getItem() {
            return item;
        }

        public void setItem(VpcItem item) {
            this.item = item;
        }
    }
}
