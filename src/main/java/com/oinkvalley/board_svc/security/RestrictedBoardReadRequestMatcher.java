package com.oinkvalley.board_svc.security;

import com.oinkvalley.board_svc.db.domain.Board;
import com.oinkvalley.board_svc.db.repository.BoardRepository;
import com.oinkvalley.board_svc.db.repository.CommentRepository;
import com.oinkvalley.board_svc.db.repository.PostRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 대상 게시판을 제한적으로만 읽을 수 있는 GET 요청과 일치합니다.
 * <ul>
 *   <li>{@link RestrictedReadLevel#USER} — {@link Board#isPrivate()} 비공개 게시판({@code USER} 역할만)</li>
 * </ul>
 * 게시판은 중첩 경로 {@code /boards/{segment}}, {@code /boards/{segment}/write},
 * {@code /boards/{segment}/{postId}}, 또는 댓글 API에서 식별합니다. (글 단건 {@code GET /posts/{id}} 는 없음.)
 */
@RequiredArgsConstructor
public class RestrictedBoardReadRequestMatcher implements RequestMatcher {

    public enum RestrictedReadLevel {
        /** 비공개 게시판 — {@code USER} 역할 필요 */
        USER
    }

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RestrictedReadLevel level;

    private static final Pattern BOARDS_POST = Pattern.compile("^/boards/([^/]+)/(\\d+)/?$");
    private static final Pattern BOARDS_WRITE = Pattern.compile("^/boards/([^/]+)/write/?$");
    private static final Pattern BOARDS_HOME = Pattern.compile("^/boards/([^/]+)/?$");
    private static final Pattern COMMENTS_ID = Pattern.compile("^/comments/(\\d+)/?$");

    @Override
    public boolean matches(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = stripContextPath(request);
        if (path.equals("/boards") || path.equals("/boards/")) {
            return false;
        }
        Optional<Long> boardId = resolveBoardId(path, request);
        if (boardId.isEmpty()) {
            return false;
        }
        return boardRepository.findById(boardId.get())
                .map(this::matchesLevel)
                .orElse(false);
    }

    private boolean matchesLevel(Board board) {
        return level == RestrictedReadLevel.USER && board.isPrivate();
    }

    private Optional<Long> resolveBoardId(String path, HttpServletRequest request) {
        if (path.startsWith("/boards/")) {
            Matcher nestedPost = BOARDS_POST.matcher(path);
            if (nestedPost.matches()) {
                return resolveBoardIdForNestedPost(nestedPost.group(1), Long.parseLong(nestedPost.group(2)));
            }
            Matcher write = BOARDS_WRITE.matcher(path);
            if (write.matches()) {
                return resolveBoardIdFromSegment(write.group(1));
            }
            Matcher home = BOARDS_HOME.matcher(path);
            if (home.matches()) {
                return resolveBoardIdFromSegment(home.group(1));
            }
        }
        if (path.startsWith("/comments")) {
            Matcher comment = COMMENTS_ID.matcher(path);
            if (comment.matches()) {
                return commentRepository.findBoardIdByCommentId(Long.parseLong(comment.group(1)));
            }
            String postIdParam = request.getParameter("postId");
            if (postIdParam != null && !postIdParam.isBlank()) {
                try {
                    long postId = Long.parseLong(postIdParam.trim());
                    return postRepository.findBoardIdByPostId(postId);
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Long> resolveBoardIdFromSegment(String segment) {
        String s = segment.trim();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        if (s.chars().allMatch(Character::isDigit)) {
            return boardRepository.findById(Long.parseLong(s)).map(Board::getId);
        }
        return boardRepository.findBySlug(s).map(Board::getId);
    }

    private Optional<Long> resolveBoardIdForNestedPost(String segment, long postId) {
        Optional<Long> boardIdOpt = postRepository.findBoardIdByPostId(postId);
        if (boardIdOpt.isEmpty()) {
            return Optional.empty();
        }
        return boardRepository.findById(boardIdOpt.get()).flatMap(board -> {
            if (!boardSegmentMatches(segment, board)) {
                return Optional.empty();
            }
            return Optional.of(board.getId());
        });
    }

    private static boolean boardSegmentMatches(String seg, Board board) {
        String s = seg.trim();
        if (s.chars().allMatch(Character::isDigit)) {
            return Long.parseLong(s) == board.getId();
        }
        return board.getSlug().equals(s);
    }

    private static String stripContextPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        if (uri.isEmpty()) {
            return "/";
        }
        if (!uri.startsWith("/")) {
            return "/" + uri;
        }
        return uri;
    }
}
