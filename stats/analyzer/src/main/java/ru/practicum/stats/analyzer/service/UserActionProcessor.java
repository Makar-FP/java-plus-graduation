package ru.practicum.stats.analyzer.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.VoidDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.analyzer.config.UserActionDeserializer;
import ru.practicum.stats.analyzer.dal.service.UserActionService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProcessor implements Runnable {

    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(100);

    private final UserActionService userActionService;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile KafkaConsumer<Void, SpecificRecordBase> consumer;

    @Autowired
    private Environment env;

    @Override
    public void run() {
        Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

        Properties properties = new Properties();
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "UserActionConsumer");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, env.getProperty("analyzer.kafka.consumer1.properties.group.id"));
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("analyzer.kafka.bootstrap-servers"));
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, VoidDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        this.consumer = new KafkaConsumer<>(properties);
        String topic = env.getProperty("analyzer.kafka.consumer1.topic");

        try {
            this.consumer.subscribe(List.of(topic));

            int processedSinceCommit = 0;
            while (running.get()) {
                ConsumerRecords<Void, SpecificRecordBase> records = this.consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                for (ConsumerRecord<Void, SpecificRecordBase> record : records) {
                    userActionService.handleRecord((UserActionAvro) record.value());
                    updateOffsets(record, currentOffsets);

                    processedSinceCommit++;
                    if (processedSinceCommit % 10 == 0) {
                        commitAsyncSafe(this.consumer, currentOffsets);
                    }
                }

                if (!records.isEmpty()) {
                    commitAsyncSafe(this.consumer, currentOffsets);
                }
            }

        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Error:", e);
        } finally {
            try {
                if (!currentOffsets.isEmpty()) {
                    this.consumer.commitSync(currentOffsets);
                }
            } finally {
                this.consumer.close();
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        KafkaConsumer<Void, SpecificRecordBase> c = this.consumer;
        if (c != null) {
            c.wakeup();
        }
    }

    private static void updateOffsets(
            ConsumerRecord<Void, SpecificRecordBase> record,
            Map<TopicPartition, OffsetAndMetadata> currentOffsets
    ) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );
    }

    private static void commitAsyncSafe(
            Consumer<Void, SpecificRecordBase> consumer,
            Map<TopicPartition, OffsetAndMetadata> currentOffsets
    ) {
        if (currentOffsets.isEmpty()) {
            return;
        }
        consumer.commitAsync(currentOffsets, (offsets, exception) -> {
            if (exception != null) {
                log.warn("Error with offsets fixations: {}", offsets, exception);
            }
        });
    }
}
