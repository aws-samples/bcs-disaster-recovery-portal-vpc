// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.util;

import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AvailabilityZone;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class Zones {

    private final AmazonEC2 sourceEc2;
    private final AmazonEC2 targetEc2;

    private Map<String, String> zoneMap;

    @Inject
    Zones(@Source AmazonEC2 sourceEc2,
          @Target AmazonEC2 targetEc2) {
        this.sourceEc2 = sourceEc2;
        this.targetEc2 = targetEc2;
    }

    public synchronized Map<String, String> getZoneMap() {
        if (zoneMap != null) {
            return zoneMap;
        }

        zoneMap = new HashMap<>();

        final var sourceZones = sourceEc2.describeAvailabilityZones().getAvailabilityZones().stream()
                .map(AvailabilityZone::getZoneName).sorted().toArray(String[]::new);
        final var targetZones = targetEc2.describeAvailabilityZones().getAvailabilityZones().stream()
                .map(AvailabilityZone::getZoneName).sorted().toArray(String[]::new);

        for (int i = 0; i < Math.min(sourceZones.length, targetZones.length); i++) {
            zoneMap.put(sourceZones[i], targetZones[i]);
        }

        if (sourceZones.length > targetZones.length) {
            for (int i = targetZones.length; i < sourceZones.length; i++) {
                zoneMap.put(sourceZones[i], targetZones[targetZones.length - 1]);
            }
        }

        return zoneMap;
    }
}
