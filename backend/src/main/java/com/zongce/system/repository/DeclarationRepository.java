package com.zongce.system.repository;

import com.zongce.system.entity.Declaration;
import com.zongce.system.entity.enums.DeclarationStatus;
import com.zongce.system.entity.enums.DeclarationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DeclarationRepository extends JpaRepository<Declaration, Long> {

    List<Declaration> findByStudentNoOrderByCreatedAtDesc(String studentNo);

    List<Declaration> findByStatusOrderByCreatedAtAsc(DeclarationStatus status);

    List<Declaration> findByStatusAndClassNameOrderByCreatedAtAsc(DeclarationStatus status, String className);

    List<Declaration> findByStatusAndTypeOrderByCreatedAtAsc(DeclarationStatus status, DeclarationType type);

    List<Declaration> findByStatusAndClassNameAndTypeOrderByCreatedAtAsc(DeclarationStatus status,
                                                                          String className,
                                                                          DeclarationType type);

    @Query("select coalesce(sum(d.points), 0) from Declaration d where d.studentNo = :studentNo and d.term = :term and d.type = :type and d.status = com.zongce.system.entity.enums.DeclarationStatus.APPROVED")
    BigDecimal sumApprovedPoints(@Param("studentNo") String studentNo,
                                 @Param("term") String term,
                                 @Param("type") DeclarationType type);
}
