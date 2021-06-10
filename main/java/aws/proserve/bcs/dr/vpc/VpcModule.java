// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.vpc;

import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.annotation.Target;
import com.amazonaws.jmespath.ObjectMapperSingleton;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;

import javax.annotation.Nullable;
import javax.inject.Singleton;

@Module
@Singleton
class VpcModule {

    @Provides
    @Singleton
    ObjectMapper objectMapper() {
        return ObjectMapperSingleton.getObjectMapper();
    }

    @Provides
    @Singleton
    AWSSecretsManager secretsManager() {
        return AWSSecretsManagerClientBuilder.defaultClient();
    }

    /**
     * @apiNote this lambda is deployed together with the dynamo table in the same region.
     */
    @Provides
    @Singleton
    AmazonDynamoDB amazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.standard().build();
    }

    @Provides
    @Singleton
    DynamoDB dynamoDB(AmazonDynamoDB amazonDynamoDB) {
        return new DynamoDB(amazonDynamoDB);
    }

    @Provides
    @Singleton
    static DynamoDBMapper dynamoMapper(AmazonDynamoDB amazonDynamoDB) {
        return new DynamoDBMapper(amazonDynamoDB,
                DynamoDBMapperConfig.builder()
                        .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.CLOBBER)
                        .build());
    }

    @Source
    @Provides
    @Singleton
    static AmazonEC2 sourceEc2(@Nullable @Source Regions region) {
        return AmazonEC2ClientBuilder.standard().withRegion(region).build();
    }

    @Target
    @Provides
    @Singleton
    static AmazonEC2 targetEc2(@Nullable @Target Regions region) {
        return AmazonEC2ClientBuilder.standard().withRegion(region).build();
    }

    @Source
    @Provides
    @Singleton
    static AmazonRDS sourceRds(@Nullable @Source Regions region) {
        return AmazonRDSClientBuilder.standard().withRegion(region).build();
    }

    @Target
    @Provides
    @Singleton
    static AmazonRDS targetRds(@Nullable @Target Regions region) {
        return AmazonRDSClientBuilder.standard().withRegion(region).build();
    }
}
