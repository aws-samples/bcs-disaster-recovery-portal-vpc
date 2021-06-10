// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc.util;

import aws.proserve.bcs.dr.lambda.annotation.Target;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Tag;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class Tagger {

    public static Tag name(String name) {
        return new Tag("Name", name);
    }

    public static Tag sourceId(String id) {
        return new Tag("drp:source:id", id);
    }

    public static Tag sourceRegion(String region) {
        return new Tag("drp:source:region", region);
    }

    public static Tag sourceZone(String zone) {
        return new Tag("drp:source:zone", zone);
    }

    public static Tag tagTime(String time) {
        return new Tag("drp:tag-time", time);
    }

    private final AmazonEC2 targetEc2;

    @Inject
    Tagger(@Target AmazonEC2 targetEc2) {
        this.targetEc2 = targetEc2;
    }

    public void tag(String id, Tag... tags) {
        tag(id, List.of(tags));
    }

    public void tag(String id, List<Tag> tags, Tag... extra) {
        targetEc2.createTags(new CreateTagsRequest()
                .withResources(id)
                .withTags(appendMetadata(tags, extra)));
    }

    private List<Tag> appendMetadata(List<Tag> tags, Tag... extra) {
        final var newTags = tags.stream()
                .filter(t -> !t.getKey().startsWith("aws:"))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.addAll(newTags, extra);
        newTags.add(tagTime(ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)));
        return newTags;
    }
}
