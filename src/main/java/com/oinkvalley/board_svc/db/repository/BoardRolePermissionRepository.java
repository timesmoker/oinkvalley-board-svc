package com.oinkvalley.board_svc.db.repository;

import com.oinkvalley.board_svc.db.domain.BoardRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BoardRolePermissionRepository
        extends JpaRepository<BoardRolePermission, BoardRolePermission.Pk> {

    List<BoardRolePermission> findByBoardIdAndRoleIn(Long boardId, Collection<String> roles);

    List<BoardRolePermission> findByRoleInAndCanReadTrue(Collection<String> roles);
}
