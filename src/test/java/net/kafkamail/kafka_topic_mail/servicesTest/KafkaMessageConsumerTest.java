package net.kafkamail.kafka_topic_mail.servicesTest;

import net.kafkamail.kafka_topic_mail.service.EmailService;
import net.kafkamail.kafka_topic_mail.service.KafkaMessageConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@EmbeddedKafka(brokerProperties = {"listeners=PLAINTEXT://localhost:9094"}, partitions = 1)
public class KafkaMessageConsumerTest {

    @Mock
    private EmailService emailService;

    private KafkaMessageConsumer kafkaMessageConsumer;
    private MockedConstruction<KafkaConsumer> mockedConsumerConstruction;

    @BeforeEach
    void setup() {
        mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class,
                (mock, context) -> {
                    ConsumerRecord<String, String> record = new ConsumerRecord<>("test-topic", 0, 0, "key", "Test message from Kafka!");
                    TopicPartition topicPartition = new TopicPartition("test-topic", 0);
                    ConsumerRecords<String, String> records = new ConsumerRecords<>(Collections.singletonMap(topicPartition, Collections.singletonList(record)));

                    // Return records once, then return empty records to stop further processing
                    Mockito.when(mock.poll(any(Duration.class)))
                            .thenReturn(records)  // First call: returns one record
                            .thenReturn(new ConsumerRecords<>(Collections.emptyMap()));  // Next calls: return empty
                });
        kafkaMessageConsumer = new KafkaMessageConsumer(
                "dummy-servers",
                "dummy-group",
                emailService
        );
        ReflectionTestUtils.setField(kafkaMessageConsumer, "topic", "test-topic");
    }

    @AfterEach
    void tearDown() {
        mockedConsumerConstruction.close();
    }

    @Test
    void testConsumerProcessesMessage() throws InterruptedException {
        Thread consumerThread = new Thread(() -> kafkaMessageConsumer.startConsumer());
        consumerThread.start();

        // ✅ Wait for a short time to let the consumer process the message
        Thread.sleep(500); // Adjust time based on processing speed

        // ✅ Verify that the emailService.sendEmail() method was called exactly **once**
        verify(emailService, times(1)).sendEmail("Test message from Kafka!");

        // ✅ Stop the consumer thread after testing (prevent infinite loop)
        consumerThread.interrupt();

    }
}