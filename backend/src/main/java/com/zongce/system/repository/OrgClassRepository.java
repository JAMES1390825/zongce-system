package com.zongce.system.repository;

import com.zongce.system.entity.OrgClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrgClassRepository extends JpaRepository<OrgClass, Long> {

    Optional<OrgClass> findByClassName(String className);

    List<OrgClass> findAllByOrderByClassNameAsc();
}
