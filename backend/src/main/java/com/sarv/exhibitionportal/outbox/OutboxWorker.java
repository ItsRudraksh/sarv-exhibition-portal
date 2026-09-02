package com.sarv.exhibitionportal.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "exhibition.outbox.schedule-enabled", havingValue = "true")
class OutboxScheduleConfig {
}

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
