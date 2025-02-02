package net.kafkamail.kafka_topic_mail.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Component
public class KafkaMessageConsumer {

    private final KafkaConsumer<String, String> consumer;
    private final EmailService emailService;

    @Value("${kafka.topic}")
    private String topic;

    public KafkaMessageConsumer(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${kafka.group-id}") String groupId,
            EmailService emailService) {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        this.consumer = new KafkaConsumer<>(props);
        this.emailService = emailService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startConsumer() {
        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be null or empty");
        }
        new Thread(() -> {
            consumer.subscribe(Collections.singletonList(topic));
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100000));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("Received message: " + record.value());
                    emailService.sendEmail(record.value());
                }
            }
        }).start();
    }
}