// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.watch;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

public class WatchEvent extends ScheduledEvent {

    String getEventName() {
        return (String) getDetail().get("eventName");
    }
}
