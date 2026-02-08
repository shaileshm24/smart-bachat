package com.ametsa.smartbachat.config;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.TopicName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PubSubConfig {

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.pubsub.topic}")
    private String topicId;

    @Bean
    public Publisher pubSubPublisher() throws Exception {
        TopicName topicName = TopicName.of(projectId, topicId);
        return Publisher.newBuilder(topicName).build();
    }
}
