package com.zongce.system.controller;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.ScorePe;
import com.zongce.system.repository.ScorePeRepository;
import com.zongce.system.service.CurrentUserService;
import com.zongce.system.service.ScoreImportService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Comparator;
import java.util.List;

@Controller
@Validated
public class TeacherPeController {

    private final ScoreImportService scoreImportService;
    private final ScorePeRepository scorePeRepository;
    private final CurrentUserService currentUserService;

    public TeacherPeController(ScoreImportService scoreImportService,
                               ScorePeRepository scorePeRepository,
                               CurrentUserService currentUserService) {
        this.scoreImportService = scoreImportService;
        this.scorePeRepository = scorePeRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/teacher/pe/import")
    public String importPage(Model model) {
        model.addAttribute("currentUser", currentUserService.mustGetCurrentUser());
        return "teacher/pe-import";
    }

    @PostMapping("/teacher/pe/import")
    public String importScores(@RequestParam("file") MultipartFile file,
                               Model model) {
        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "请选择CSV文件");
            return importPage(model);
        }
        ScoreImportService.ImportResult result = scoreImportService.importPeScores(
                file,
                currentUserService.mustGetCurrentUser().getUsername()
        );
        model.addAttribute("success", "导入完成：成功 " + result.success() + " 条，失败 " + result.failed() + " 条");
        model.addAttribute("currentUser", currentUserService.mustGetCurrentUser());
        return "teacher/pe-import";
    }

    @GetMapping("/teacher/pe/edit")
    public String editPage(@RequestParam(required = false) Long id, Model model) {
        ScorePe row;
        boolean editing = id != null;
        if (editing) {
            row = scorePeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("记录不存在，ID=" + id));
        } else {
            row = new ScorePe();
            row.setTerm(Year.now().getValue() + "-1");
        }

        model.addAttribute("row", row);
        model.addAttribute("editing", editing);
        model.addAttribute("currentUser", currentUserService.mustGetCurrentUser());
        return "teacher/pe-edit";
    }

    @PostMapping("/teacher/pe/edit")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam @NotBlank String studentNo,
                       @RequestParam @NotBlank String studentName,
                       @RequestParam @NotBlank String className,
                       @RequestParam @NotBlank String term,
                       @RequestParam BigDecimal score,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "体育分必须大于等于 0");
            return refillEditModel(id, studentNo, studentName, className, term, score, model);
        }

        AppUser operator = currentUserService.mustGetCurrentUser();
        ScorePe row = (id == null)
                ? new ScorePe()
                : scorePeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("记录不存在，ID=" + id));

        row.setStudentNo(studentNo.trim());
        row.setStudentName(studentName.trim());
        row.setClassName(className.trim());
        row.setTerm(term.trim());
        row.setScore(score);
        row.setCreatedBy(operator.getUsername());
        scorePeRepository.save(row);

        redirectAttributes.addFlashAttribute("success", id == null ? "体育成绩录入成功" : "体育成绩修改成功");
        return "redirect:/teacher/pe/list?className=" + row.getClassName() + "&term=" + row.getTerm();
    }

    @GetMapping("/teacher/pe/list")
    public String list(@RequestParam(required = false) String className,
                       @RequestParam(required = false) String term,
                       Model model) {
        List<ScorePe> rows;
        if (className != null && className.isBlank() == false && term != null && term.isBlank() == false) {
            rows = scorePeRepository.findByClassNameAndTermOrderByStudentNoAsc(className.trim(), term.trim());
        } else if (term != null && term.isBlank() == false) {
            rows = scorePeRepository.findByTermOrderByClassNameAscStudentNoAsc(term.trim());
        } else {
            rows = scorePeRepository.findAll().stream()
                    .sorted(Comparator.comparing(ScorePe::getClassName, Comparator.nullsLast(String::compareTo))
                            .thenComparing(ScorePe::getStudentNo, Comparator.nullsLast(String::compareTo)))
                    .toList();
        }

        model.addAttribute("rows", rows);
        model.addAttribute("className", className == null ? "" : className);
        model.addAttribute("term", term == null ? "" : term);
        model.addAttribute("currentUser", currentUserService.mustGetCurrentUser());
        return "teacher/pe-list";
    }

    private String refillEditModel(Long id,
                                   String studentNo,
                                   String studentName,
                                   String className,
                                   String term,
                                   BigDecimal score,
                                   Model model) {
        ScorePe row = new ScorePe();
        row.setId(id);
        row.setStudentNo(studentNo);
        row.setStudentName(studentName);
        row.setClassName(className);
        row.setTerm(term);
        row.setScore(score);

        model.addAttribute("row", row);
        model.addAttribute("editing", id != null);
        model.addAttribute("currentUser", currentUserService.mustGetCurrentUser());
        return "teacher/pe-edit";
    }
}
