package com.zongce.system.controller;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.Declaration;
import com.zongce.system.entity.ScoreResult;
import com.zongce.system.entity.enums.DeclarationStatus;
import com.zongce.system.entity.enums.DeclarationType;
import com.zongce.system.repository.DeclarationRepository;
import com.zongce.system.repository.ScoreResultRepository;
import com.zongce.system.service.CurrentUserService;
import com.zongce.system.service.FileStorageService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Controller
@Validated
public class StudentController {

    private final CurrentUserService currentUserService;
    private final DeclarationRepository declarationRepository;
    private final ScoreResultRepository scoreResultRepository;
    private final FileStorageService fileStorageService;

    public StudentController(CurrentUserService currentUserService,
                             DeclarationRepository declarationRepository,
                             ScoreResultRepository scoreResultRepository,
                             FileStorageService fileStorageService) {
        this.currentUserService = currentUserService;
        this.declarationRepository = declarationRepository;
        this.scoreResultRepository = scoreResultRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/student/declare")
    public String declarePage(Model model) {
        prepareDeclarePage(model, null, "/student/declare", "德育 / 技能申报提交");
        return "student/declare";
    }

    @GetMapping("/student/declare/moral")
    public String moralDeclarePage(Model model) {
        prepareDeclarePage(model, DeclarationType.MORAL, "/student/declare/moral", "德育分申报");
        return "student/declare";
    }

    @GetMapping("/student/declare/skill")
    public String skillDeclarePage(Model model) {
        prepareDeclarePage(model, DeclarationType.SKILL, "/student/declare/skill", "技能分申报");
        return "student/declare";
    }

    @PostMapping("/student/declare")
    public String submitDeclaration(@RequestParam DeclarationType type,
                                    @RequestParam @NotBlank String term,
                                    @RequestParam @NotBlank String itemName,
                                    @RequestParam BigDecimal points,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(required = false) MultipartFile attachment,
                                    Model model) {
        return submit(type, term, itemName, points, description, attachment, "/student/declare", model);
    }

    @PostMapping({"/student/declare/moral", "/student/declarations/moral"})
    public String submitMoral(@RequestParam @NotBlank String term,
                              @RequestParam @NotBlank String itemName,
                              @RequestParam BigDecimal points,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) MultipartFile attachment,
                              Model model) {
        return submit(DeclarationType.MORAL, term, itemName, points, description, attachment, "/student/declare/moral", model);
    }

    @PostMapping({"/student/declare/skill", "/student/declarations/skill"})
    public String submitSkill(@RequestParam @NotBlank String term,
                              @RequestParam @NotBlank String itemName,
                              @RequestParam BigDecimal points,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) MultipartFile attachment,
                              Model model) {
        return submit(DeclarationType.SKILL, term, itemName, points, description, attachment, "/student/declare/skill", model);
    }

    @GetMapping({"/student/history", "/student/declare/history"})
    public String history(Model model) {
        AppUser student = currentUserService.mustGetCurrentUser();
        List<Declaration> rows = declarationRepository.findByStudentNoOrderByCreatedAtDesc(student.getUsername());
        model.addAttribute("rows", rows);
        model.addAttribute("currentUser", student);
        return "student/history";
    }

    @GetMapping({"/student/scores", "/student/my-score"})
    public String myScores(Model model) {
        AppUser student = currentUserService.mustGetCurrentUser();
        List<ScoreResult> rows = scoreResultRepository.findByStudentNoOrderByTermDesc(student.getUsername());
        model.addAttribute("rows", rows);
        model.addAttribute("currentUser", student);
        return "student/scores";
    }

    private String submit(DeclarationType type,
                          String term,
                          String itemName,
                          BigDecimal points,
                          String description,
                          MultipartFile attachment,
                          String backPath,
                          Model model) {
        AppUser student = currentUserService.mustGetCurrentUser();
        if (points == null || points.compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("error", "分值必须大于0");
            prepareDeclarePage(model, type, backPath, type == DeclarationType.MORAL ? "德育分申报" : "技能分申报");
            return "student/declare";
        }

        String attachmentPath = null;
        if (attachment != null && attachment.isEmpty() == false) {
            attachmentPath = fileStorageService.store(attachment, "declaration", student.getUsername());
        }

        Declaration declaration = new Declaration();
        declaration.setStudentNo(student.getUsername());
        declaration.setStudentName(student.getName());
        declaration.setClassName(student.getClassName());
        declaration.setType(type);
        declaration.setTerm(term.trim());
        declaration.setItemName(itemName.trim());
        declaration.setPoints(points);
        declaration.setDescription(description);
        declaration.setAttachmentPath(attachmentPath);
        declaration.setStatus(DeclarationStatus.PENDING);
        declarationRepository.save(declaration);

        model.addAttribute("success", "提交成功，等待辅导员审核");
        prepareDeclarePage(model, type, backPath, type == DeclarationType.MORAL ? "德育分申报" : "技能分申报");
        return "student/declare";
    }

    private void prepareDeclarePage(Model model,
                                    DeclarationType fixedType,
                                    String submitAction,
                                    String pageTitle) {
        model.addAttribute("types", DeclarationType.values());
        model.addAttribute("fixedType", fixedType);
        model.addAttribute("submitAction", submitAction);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("currentUser", currentUserService.mustGetCurrentUser());
    }
}
