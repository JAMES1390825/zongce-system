package com.zongce.system.config;

import com.zongce.system.entity.OrgClass;
import com.zongce.system.entity.OrgDepartment;
import com.zongce.system.entity.ScoreItemCatalog;
import com.zongce.system.entity.enums.ScoreItemCategory;
import com.zongce.system.repository.OrgClassRepository;
import com.zongce.system.repository.OrgDepartmentRepository;
import com.zongce.system.repository.ScoreItemCatalogRepository;
import com.zongce.system.service.CalcRuleService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemSetupInitializer {

    @Bean
    public CommandLineRunner initSystemSetup(OrgClassRepository orgClassRepository,
                                             OrgDepartmentRepository orgDepartmentRepository,
                                             ScoreItemCatalogRepository scoreItemCatalogRepository,
                                             CalcRuleService calcRuleService) {
        return args -> {
            ensureClass(orgClassRepository, "软件221", "软件工程");
            ensureClass(orgClassRepository, "软件222", "软件工程");

            ensureDepartment(orgDepartmentRepository, "管理部");
            ensureDepartment(orgDepartmentRepository, "体育部");
            ensureDepartment(orgDepartmentRepository, "学习部");
            ensureDepartment(orgDepartmentRepository, "辅导员");

            ensureScoreItem(scoreItemCatalogRepository, "STUDY-TOTAL", "智育总分", ScoreItemCategory.STUDY, false, 0);
            ensureScoreItem(scoreItemCatalogRepository, "STUDY-MATH-A1", "高等数学A1", ScoreItemCategory.STUDY, true, 10);
            ensureScoreItem(scoreItemCatalogRepository, "STUDY-DIGITAL-CIRCUIT", "数字电路", ScoreItemCategory.STUDY, true, 20);
            ensureScoreItem(scoreItemCatalogRepository, "STUDY-COLLEGE-ENGLISH", "大学英语", ScoreItemCategory.STUDY, true, 30);
            ensureScoreItem(scoreItemCatalogRepository, "STUDY-PROGRAM-DESIGN", "程序设计基础", ScoreItemCategory.STUDY, true, 40);

            ensureScoreItem(scoreItemCatalogRepository, "PE-TOTAL", "体育总分", ScoreItemCategory.PE, false, 0);
            ensureScoreItem(scoreItemCatalogRepository, "PE-1000M", "1000m长跑", ScoreItemCategory.PE, true, 10);
            ensureScoreItem(scoreItemCatalogRepository, "PE-DAILY", "平时分", ScoreItemCategory.PE, true, 20);
            ensureScoreItem(scoreItemCatalogRepository, "PE-VITAL-CAPACITY", "肺活量", ScoreItemCategory.PE, false, 30);
            ensureScoreItem(scoreItemCatalogRepository, "PE-SIT-AND-REACH", "坐位体前屈", ScoreItemCategory.PE, false, 40);

            ensureScoreItem(scoreItemCatalogRepository, "MORAL-VOLUNTEER", "院级志愿服务", ScoreItemCategory.MORAL, false, 10);
            ensureScoreItem(scoreItemCatalogRepository, "MORAL-CLASS-CADRE", "班干部服务", ScoreItemCategory.MORAL, false, 20);

            ensureScoreItem(scoreItemCatalogRepository, "SKILL-CERT-CET4", "英语四级证书", ScoreItemCategory.SKILL, false, 10);
            ensureScoreItem(scoreItemCatalogRepository, "SKILL-COMPETITION-PROVINCIAL", "省级竞赛获奖", ScoreItemCategory.SKILL, false, 20);

            // 确保系统存在一条可用规则
            calcRuleService.getCurrentRule();
        };
    }

    private void ensureClass(OrgClassRepository repository,
                             String className,
                             String majorName) {
        OrgClass row = repository.findByClassName(className).orElseGet(OrgClass::new);
        row.setClassName(className);
        row.setMajorName(majorName);
        row.setEnabled(true);
        repository.save(row);
    }

    private void ensureDepartment(OrgDepartmentRepository repository,
                                  String departmentName) {
        OrgDepartment row = repository.findByDepartmentName(departmentName).orElseGet(OrgDepartment::new);
        row.setDepartmentName(departmentName);
        row.setEnabled(true);
        repository.save(row);
    }

    private void ensureScoreItem(ScoreItemCatalogRepository repository,
                                 String itemCode,
                                 String itemName,
                                 ScoreItemCategory category,
                                 boolean requiredFlag,
                                 int displayOrder) {
        ScoreItemCatalog row = repository.findByItemCode(itemCode).orElseGet(ScoreItemCatalog::new);
        row.setItemCode(itemCode);
        row.setItemName(itemName);
        row.setCategory(category);
        row.setRequiredFlag(requiredFlag);
        row.setDisplayOrder(displayOrder);
        row.setEnabled(true);
        repository.save(row);
    }
}
