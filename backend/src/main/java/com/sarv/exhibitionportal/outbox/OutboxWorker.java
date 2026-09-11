package com.sarv.exhibitionportal.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Enables the outbox poller when {@code exhibition.outbox.schedule-enabled=true}. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "exhibition.outbox.schedule-enabled", havingValue = "true")
class OutboxScheduleConfig {
}

/** Polls pending deliveries and writes local stub JSON. Failures retry; inquiries stay. */
@Component
public class OutboxWorker {

    private final OutboxService outbox;

    public OutboxWorker(OutboxService outbox) {
        this.outbox = outbox;
    }

    @Scheduled(fixedDelayString = "${exhibition.outbox.poll-ms:2000}")
    public void poll() {
        outbox.processDue();
    }
}
