// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.watch;

import aws.proserve.bcs.dr.lambda.BoolHandler;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Watch implements BoolHandler<WatchEvent> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean handleRequest(WatchEvent event, Context context) {
        log.debug("Received event {}", event);

        return true;
    }
}
