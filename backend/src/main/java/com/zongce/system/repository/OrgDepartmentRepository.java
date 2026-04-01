package com.zongce.system.repository;

import com.zongce.system.entity.OrgDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrgDepartmentRepository extends JpaRepository<OrgDepartment, Long> {

    Optional<OrgDepartment> findByDepartmentName(String departmentName);

    List<OrgDepartment> findAllByOrderByDepartmentNameAsc();
}
