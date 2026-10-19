package com.oinkvalley.board_svc.messaging.outbox;

import com.oinkvalley.board_svc.db.domain.BoardOutbox;
import com.oinkvalley.board_svc.db.repository.BoardOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 도메인 변경과 같은 트랜잭션에 "보낼 이벤트"를 기록. 실제 발행은 {@link OutboxRelay}.
 * 트랜잭션 밖에서 부르면 실패한다(MANDATORY) — 커밋과 이벤트가 따로 놀지 않게.
 */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final BoardOutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String subject, Map<String, Object> payload) {
        outboxRepository.save(BoardOutbox.builder()
                .subject(subject)
                .payload(payload)
                .build());
    }
}
