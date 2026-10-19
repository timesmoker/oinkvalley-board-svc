package com.oinkvalley.board_svc.config;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Nats;
import io.nats.client.Options;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

/**
 * NATS 연결·발행. Subject는 발행자가 넘김 (도메인 이벤트 주소).
 * 수신·알림 정책은 notification-svc 책임 — board는 사실 JSON만 실음.
 */
@Component
public class NatsConfig {

    private static final Logger log = LoggerFactory.getLogger(NatsConfig.class);
    private static final JsonMapper JSON = JsonMapper.shared();

    /** 첫 연결 전엔 null, 끊긴 동안엔 상태가 CONNECTED 가 아니다 — 쓰기 전에 {@link #isConnected()}. */
    private volatile Connection connection;

    /**
     * 기동을 막지 않고 백그라운드에서 연결한다. NATS 가 늦게 떠도(재부팅 순서) 붙을 때까지 재시도하고,
     * 끊기면 다시 붙는다 — 연결 실패로 기능을 끄지 않는다.
     */
    public NatsConfig(@Value("${nats.url}") String natsUrl, @Value("${nats.reconnect-wait}") Duration reconnectWait) {
        Options options = new Options.Builder()
                .server(natsUrl)
                .connectionName("board-svc")
                .maxReconnects(-1)
                .reconnectWait(reconnectWait)
                .connectionListener((conn, event) -> {
                    if (event == ConnectionListener.Events.CONNECTED || event == ConnectionListener.Events.RECONNECTED) {
                        connection = conn;
                        log.info("NATS {} url={}", event, natsUrl);
                    } else if (event == ConnectionListener.Events.DISCONNECTED) {
                        log.warn("NATS disconnected — reconnecting url={}", natsUrl);
                    }
                })
                .build();
        try {
            Nats.connectAsynchronously(options, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("NATS connect interrupted", e);
        }
    }

    public boolean isConnected() {
        Connection nc = connection;
        return nc != null && nc.getStatus() == Connection.Status.CONNECTED;
    }

    /** subject 로 JSON 발행. 연결이 없으면 버린다 (경고 로그). */
    public void publish(String subject, Map<String, Object> body) {
        if (subject == null || subject.isBlank() || body == null || body.isEmpty()) {
            return;
        }
        Connection connection = this.connection;
        if (!isConnected()) {
            log.warn("NATS not connected — event dropped subject={}", subject);
            return;
        }
        try {
            connection.publish(subject, JSON.writeValueAsBytes(body));
        } catch (Exception e) {
            log.warn("NATS publish failed subject={}: {}", subject, e.toString());
        }
    }

    @PreDestroy
    void close() {
        Connection connection = this.connection;
        if (connection == null) {
            return;
        }
        try {
            connection.flush(Duration.ofSeconds(2));
            connection.close();
        } catch (Exception e) {
            log.debug("NATS close: {}", e.toString());
        }
    }
}
