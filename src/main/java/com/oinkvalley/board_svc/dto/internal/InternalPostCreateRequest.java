package com.oinkvalley.board_svc.dto.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** bubble-pal {@code BoardInternalPostRequest} 와 동일 계약. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InternalPostCreateRequest(
        @NotNull @Positive @JsonProperty("boardId") Long boardId,
        @NotNull @Positive @JsonProperty("authorUserId") Long authorUserId,
        @NotBlank @Size(max = 100) String title,
        @NotBlank String text,
        @JsonProperty("sourceUrl") String sourceUrl,
        @JsonProperty("characterId") String characterId,
        @JsonProperty("authorType") String authorType,
        Map<String, Object> metadata
) {
}
