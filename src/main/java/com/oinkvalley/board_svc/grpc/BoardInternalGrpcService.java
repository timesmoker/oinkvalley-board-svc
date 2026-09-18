package com.oinkvalley.board_svc.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import com.oinkvalley.board.v1.BoardInternalServiceGrpc;
import com.oinkvalley.board.v1.CreatePostRequest;
import com.oinkvalley.board.v1.CreatePostResponse;
import com.oinkvalley.board_svc.dto.internal.InternalPostCreateRequest;
import com.oinkvalley.board_svc.dto.internal.InternalPostCreateResponse;
import com.oinkvalley.board_svc.service.PostService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** 기존 {@code POST /internal/posts} 대체. Service 레이어는 그대로 호출. */
@GrpcService
@RequiredArgsConstructor
public class BoardInternalGrpcService extends BoardInternalServiceGrpc.BoardInternalServiceImplBase {

	private static final JsonMapper JSON = JsonMapper.shared();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

	private final PostService postService;

	@Override
	public void createPost(CreatePostRequest request, StreamObserver<CreatePostResponse> responseObserver) {
		try {
			InternalPostCreateResponse created = postService.createInternal(toDto(request));
			responseObserver.onNext(CreatePostResponse.newBuilder().setPostId(created.postId()).build());
			responseObserver.onCompleted();
		} catch (ResponseStatusException e) {
			responseObserver.onError(toGrpcStatus(e).withDescription(e.getReason()).asRuntimeException());
		} catch (Exception e) {
			responseObserver.onError(
					Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
		}
	}

	private static InternalPostCreateRequest toDto(CreatePostRequest request)
			throws InvalidProtocolBufferException {
		return new InternalPostCreateRequest(
				request.getBoardId(),
				request.getAuthorUserId(),
				request.getTitle(),
				request.getText(),
				blankToNull(request.getSourceUrl()),
				blankToNull(request.getCharacterId()),
				blankToNull(request.getAuthorType()),
				structToMap(request.getMetadata()));
	}

	private static Map<String, Object> structToMap(Struct metadata) throws InvalidProtocolBufferException {
		if (metadata == null || metadata.getFieldsCount() == 0) {
			return null;
		}
		return JSON.readValue(JsonFormat.printer().print(metadata), MAP_TYPE);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private static Status toGrpcStatus(ResponseStatusException e) {
		if (e.getStatusCode() instanceof HttpStatus http) {
			return switch (http) {
				case BAD_REQUEST -> Status.INVALID_ARGUMENT;
				case NOT_FOUND -> Status.NOT_FOUND;
				case FORBIDDEN -> Status.PERMISSION_DENIED;
				case CONFLICT -> Status.ALREADY_EXISTS;
				default -> Status.INTERNAL;
			};
		}
		return Status.INTERNAL;
	}
}
