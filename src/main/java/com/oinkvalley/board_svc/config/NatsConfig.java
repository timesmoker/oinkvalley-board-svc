package com.oinkvalley.board_svc.config;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.KeyValue;
import io.nats.client.KeyValueManagement;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.PublishOptions;
import io.nats.client.api.KeyValueConfiguration;
import io.nats.client.api.StorageType;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NATS 연결·발행. Subject는 발행자가 넘김 (도메인 이벤트 주소).
 * 수신·알림 정책은 notification-svc 책임 — board는 사실 JSON만 실음.
 * 발행은 outbox 발행기({@code OutboxRelay})만 호출한다 — 도메인 코드는 outbox 에 기록.
 */
@Component
public class NatsConfig {

    private static final Logger log = LoggerFactory.getLogger(NatsConfig.class);
    private static final JsonMapper JSON = JsonMapper.shared();

    /** publishAcked 가 스트림 ack 를 기다리는 시간 */
    private final Duration publishTimeout;
    /** (재)연결될 때마다 실행 — 끊긴 동안 밀린 일을 바로 따라잡게 */
    private final List<Runnable> onConnected = new CopyOnWriteArrayList<>();
    /** 첫 연결 전엔 null, 끊긴 동안엔 상태가 CONNECTED 가 아니다 — 쓰기 전에 {@link #isConnected()}. */
    private volatile Connection connection;

    /**
     * 기동을 막지 않고 백그라운드에서 연결한다. NATS 가 늦게 떠도(재부팅 순서) 붙을 때까지 재시도하고,
     * 끊기면 다시 붙는다 — 연결 실패로 기능을 끄지 않는다.
     */
    public NatsConfig(@Value("${nats.url}") String natsUrl, @Value("${nats.reconnect-wait}") Duration reconnectWait,
            @Value("${nats.publish-timeout-ms}") long publishTimeoutMs) {
        this.publishTimeout = Duration.ofMillis(publishTimeoutMs);
        Options options = new Options.Builder()
                .server(natsUrl)
                .connectionName("board-svc")
                .maxReconnects(-1)
                .reconnectWait(reconnectWait)
                .connectionListener((conn, event) -> {
                    if (event == ConnectionListener.Events.CONNECTED || event == ConnectionListener.Events.RECONNECTED) {
                        connection = conn;
                        log.info("NATS {} url={}", event, natsUrl);
                        onConnected.forEach(task -> Thread.ofVirtual().start(task));
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

    /** (재)연결 시 실행할 작업 등록. 이미 연결돼 있으면 바로 한 번 실행한다. */
    public void onConnected(Runnable task) {
        onConnected.add(task);
        if (isConnected()) {
            Thread.ofVirtual().start(task);
        }
    }

    public boolean isConnected() {
        Connection nc = connection;
        return nc != null && nc.getStatus() == Connection.Status.CONNECTED;
    }

    /**
     * JetStream 발행 후 스트림 저장 확인(ack)까지 기다린다. 실패하면 예외 — 호출자가 재시도.
     * messageId 가 같으면 스트림 중복 제거 창 안에서 한 번만 저장된다.
     */
    public void publishAcked(String subject, Map<String, Object> body, String messageId) throws Exception {
        Connection connection = this.connection;
        if (!isConnected()) {
            throw new IllegalStateException("NATS not connected");
        }
        JetStream js = connection.jetStream();
        PublishOptions options = PublishOptions.builder()
                .messageId(messageId)
                .streamTimeout(publishTimeout)
                .build();
        js.publish(subject, JSON.writeValueAsBytes(body), options);
    }

    /** JetStream KV 버킷 (없으면 생성, 키당 최신 값 1개만 유지). */
    public KeyValue keyValueBucket(String bucket) throws Exception {
        Connection connection = this.connection;
        if (!isConnected()) {
            throw new IllegalStateException("NATS not connected");
        }
        KeyValueManagement kvm = connection.keyValueManagement();
        try {
            kvm.getStatus(bucket);
        } catch (JetStreamApiException notFound) {
            kvm.create(KeyValueConfiguration.builder()
                    .name(bucket)
                    .storageType(StorageType.File)
                    .maxHistoryPerKey(1)
                    .build());
        }
        return connection.keyValue(bucket);
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
