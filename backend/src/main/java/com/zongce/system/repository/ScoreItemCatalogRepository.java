package com.zongce.system.repository;

import com.zongce.system.entity.ScoreItemCatalog;
import com.zongce.system.entity.enums.ScoreItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScoreItemCatalogRepository extends JpaRepository<ScoreItemCatalog, Long> {

    Optional<ScoreItemCatalog> findByItemCode(String itemCode);

    List<ScoreItemCatalog> findByEnabledTrueOrderByCategoryAscDisplayOrderAscItemCodeAsc();

    List<ScoreItemCatalog> findByCategoryInAndEnabledTrueOrderByCategoryAscDisplayOrderAscItemCodeAsc(
            Collection<ScoreItemCategory> categories
    );
}
