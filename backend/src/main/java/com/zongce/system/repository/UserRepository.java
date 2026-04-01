package com.zongce.system.repository;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    List<AppUser> findByRole(RoleType role);

    List<AppUser> findByRoleOrderByUsernameAsc(RoleType role);

    List<AppUser> findByRoleAndClassNameAndEnabledTrue(RoleType role, String className);

    List<AppUser> findByRoleAndEnabledTrue(RoleType role);

    List<AppUser> findByRoleAndClassNameContainingIgnoreCaseOrderByUsernameAsc(RoleType role, String className);

    List<AppUser> findByRoleInOrderByRoleAscUsernameAsc(List<RoleType> roles);

    List<AppUser> findByRoleInAndDepartmentContainingIgnoreCaseOrderByRoleAscUsernameAsc(List<RoleType> roles,
                                                                                          String department);
}
