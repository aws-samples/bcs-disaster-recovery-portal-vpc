// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.vpc;

import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.lambda.StringHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import aws.proserve.bcs.dr.project.Component;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.ProjectFinder;
import aws.proserve.bcs.dr.project.Region;
import aws.proserve.bcs.dr.vpc.VpcComponent;
import aws.proserve.bcs.dr.vpc.VpcProject;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

public class CreateVpcProject implements StringHandler<Map<String, Object>> {

    @Override
    public String handleRequest(Map<String, Object> request, Context context) {
        return VpcComponent.build(request).createProject().create(request);
    }

    @Singleton
    public static class Worker {
        private final Logger log = LoggerFactory.getLogger(getClass());
        private final ProjectFinder finder;
        private final Regions sourceRegion;
        private final Regions targetRegion;

        @Inject
        Worker(ProjectFinder finder,
               @Nullable @Source Regions sourceRegion,
               @Nullable @Target Regions targetRegion) {
            this.finder = finder;
            this.sourceRegion = sourceRegion;
            this.targetRegion = targetRegion;
        }

        private String create(Map<String, Object> request) {
            final var name = (String) request.get("name");

            if (isNameUsed(name)) {
                throw new PortalException("VPC project name is used");
            }

            final var vpcProject = new VpcProject();
            vpcProject.setItems(List.of());

            final var project = new Project();
            project.setName(name);
            project.setType(Component.VPC);
            project.setSourceRegion(new Region(sourceRegion));
            project.setTargetRegion(new Region(targetRegion));
            project.setVpcProject(vpcProject);

            log.debug("Save VPC project [{}]", project.getName());
            finder.save(project);
            return project.getId();
        }

        private boolean isNameUsed(String name) {
            return finder.findByType(Component.VPC)
                    .stream()
                    .map(Project::getName)
                    .anyMatch(name::equals);
        }
    }
}
