package com.oinkvalley.board_svc.messaging.outbox;

import com.oinkvalley.board_svc.config.NatsConfig;
import com.oinkvalley.board_svc.db.domain.BoardOutbox;
import com.oinkvalley.board_svc.db.repository.BoardOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * outbox 발행기. 미발행 행을 id 순으로 JetStream 에 발행하고, 스트림 ack 를 받은 행만 published_at 기록.
 * 실패한 행은 그대로 남아 다음 주기에 재시도 (순서 유지를 위해 실패 지점에서 멈춤).
 * 최소 1회 전달 — messageId("board-outbox-{id}")로 스트림 중복 제거, 수신자도 중복에 안전해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final BoardOutboxRepository outboxRepository;
    private final NatsConfig natsConfig;

    /** 한 주기에 발행하는 최대 행 수 */
    @Value("${board.outbox.batch-size}")
    private int batchSize;

    /** 발행 끝난 행을 남겨 두는 기간 */
    @Value("${board.outbox.retention-days}")
    private int retentionDays;

    @Scheduled(fixedDelayString = "${board.outbox.relay-delay-ms}")
    @Transactional
    public void relay() {
        if (!natsConfig.isConnected()) {
            return;
        }
        List<BoardOutbox> pending = outboxRepository.lockPending(batchSize);
        for (BoardOutbox event : pending) {
            try {
                natsConfig.publishAcked(event.getSubject(), event.getPayload(), "board-outbox-" + event.getId());
                event.markPublished();
            } catch (Exception e) {
                log.warn("outbox publish failed id={} subject={}: {}", event.getId(), event.getSubject(), e.toString());
                return;
            }
        }
    }

    /** 발행 끝난 행 정리. */
    @Scheduled(cron = "${board.outbox.cleanup-cron}", zone = "Asia/Seoul")
    @Transactional
    public void cleanup() {
        int deleted = outboxRepository.deletePublishedBefore(Instant.now().minus(Duration.ofDays(retentionDays)));
        if (deleted > 0) {
            log.info("outbox cleanup deleted={}", deleted);
        }
    }
}
