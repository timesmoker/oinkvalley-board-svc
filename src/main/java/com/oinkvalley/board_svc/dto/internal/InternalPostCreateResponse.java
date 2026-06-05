package com.oinkvalley.board_svc.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InternalPostCreateResponse(@JsonProperty("postId") Long postId) {
}
