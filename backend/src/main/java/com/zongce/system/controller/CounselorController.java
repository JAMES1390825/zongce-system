package com.zongce.system.controller;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.Declaration;
import com.zongce.system.entity.ScoreResult;
import com.zongce.system.entity.enums.DeclarationStatus;
import com.zongce.system.entity.enums.DeclarationType;
import com.zongce.system.repository.DeclarationRepository;
import com.zongce.system.repository.ScoreResultRepository;
import com.zongce.system.service.CurrentUserService;
import com.zongce.system.service.ScoreRecalcService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Year;
import java.util.List;

@Controller
@Validated
public class CounselorController {

    private final CurrentUserService currentUserService;
    private final DeclarationRepository declarationRepository;
    private final ScoreRecalcService scoreRecalcService;
    private final ScoreResultRepository scoreResultRepository;

    public CounselorController(CurrentUserService currentUserService,
                               DeclarationRepository declarationRepository,
                               ScoreRecalcService scoreRecalcService,
                               ScoreResultRepository scoreResultRepository) {
        this.currentUserService = currentUserService;
        this.declarationRepository = declarationRepository;
        this.scoreRecalcService = scoreRecalcService;
        this.scoreResultRepository = scoreResultRepository;
    }

    @GetMapping("/counselor/reviews")
    public String reviews(@RequestParam(defaultValue = "PENDING") DeclarationStatus status,
                          Model model) {
        return renderReviews(status, null, "/counselor/reviews", model);
    }

    @GetMapping("/counselor/review/moral")
    public String moralReviews(@RequestParam(defaultValue = "PENDING") DeclarationStatus status,
                               Model model) {
        return renderReviews(status, DeclarationType.MORAL, "/counselor/review/moral", model);
    }

    @GetMapping("/counselor/review/skill")
    public String skillReviews(@RequestParam(defaultValue = "PENDING") DeclarationStatus status,
                               Model model) {
        return renderReviews(status, DeclarationType.SKILL, "/counselor/review/skill", model);
    }

    @PostMapping("/counselor/reviews/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String returnTo) {
        AppUser counselor = currentUserService.mustGetCurrentUser();
        Declaration row = declarationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        ensureCounselorDataScope(counselor, row);

        row.setStatus(DeclarationStatus.APPROVED);
        row.setReviewer(counselor.getUsername());
        row.setReviewComment("审核通过");
        declarationRepository.save(row);
        return "redirect:" + safeReturn(returnTo);
    }

    @PostMapping("/counselor/reviews/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String comment,
                         @RequestParam(required = false) String returnTo) {
        AppUser counselor = currentUserService.mustGetCurrentUser();
        Declaration row = declarationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        ensureCounselorDataScope(counselor, row);

        row.setStatus(DeclarationStatus.REJECTED);
        row.setReviewer(counselor.getUsername());
        row.setReviewComment(comment == null || comment.isBlank() ? "材料不完整" : comment.trim());
        declarationRepository.save(row);
        return "redirect:" + safeReturn(returnTo);
    }

    @GetMapping("/counselor/class-summary")
    public String classSummary(@RequestParam(required = false) String className,
                               @RequestParam(required = false) String term,
                               Model model) {
        AppUser counselor = currentUserService.mustGetCurrentUser();
        String scopedClass = clean(counselor.getClassName());
        String finalClass = StringUtils.hasText(scopedClass) ? scopedClass : clean(className);
        String finalTerm = StringUtils.hasText(term)
                ? term.trim()
                : Year.now().getValue() + "-1";

        List<ScoreResult> rows;
        if (StringUtils.hasText(finalClass) == false) {
            rows = List.of();
        } else {
            rows = scoreResultRepository.findByClassNameAndTermOrderByRankNoAsc(finalClass, finalTerm);
        }

        model.addAttribute("rows", rows);
        model.addAttribute("className", finalClass == null ? "" : finalClass);
        model.addAttribute("term", finalTerm);
        model.addAttribute("scopedClass", scopedClass);
        model.addAttribute("currentUser", counselor);
        return "counselor/class-summary";
    }

    @PostMapping("/counselor/class-summary/recalc")
    public String recalc(@RequestParam(required = false) String className,
                         @RequestParam @NotBlank String term,
                         RedirectAttributes redirectAttributes) {
        AppUser counselor = currentUserService.mustGetCurrentUser();
        String scopedClass = clean(counselor.getClassName());
        String finalClass = StringUtils.hasText(scopedClass) ? scopedClass : clean(className);

        if (StringUtils.hasText(finalClass) == false) {
            throw new IllegalArgumentException("请输入班级后再重算");
        }
        if (StringUtils.hasText(scopedClass) && StringUtils.hasText(className)
                && scopedClass.equals(className.trim()) == false) {
            throw new IllegalArgumentException("你只能重算自己负责的班级：" + scopedClass);
        }

        scoreRecalcService.recalcClass(finalClass, term.trim());
        redirectAttributes.addFlashAttribute("success", "重算完成：班级 " + finalClass + "，学期 " + term.trim());
        return "redirect:/counselor/class-summary?className=" + finalClass + "&term=" + term.trim();
    }

    private String renderReviews(DeclarationStatus status,
                                 DeclarationType type,
                                 String basePath,
                                 Model model) {
        AppUser counselor = currentUserService.mustGetCurrentUser();
        String scopedClass = clean(counselor.getClassName());
        List<Declaration> rows;
        if (StringUtils.hasText(scopedClass)) {
            rows = (type == null)
                    ? declarationRepository.findByStatusAndClassNameOrderByCreatedAtAsc(status, scopedClass)
                    : declarationRepository.findByStatusAndClassNameAndTypeOrderByCreatedAtAsc(status, scopedClass, type);
        } else {
            rows = (type == null)
                    ? declarationRepository.findByStatusOrderByCreatedAtAsc(status)
                    : declarationRepository.findByStatusAndTypeOrderByCreatedAtAsc(status, type);
        }

        model.addAttribute("rows", rows);
        model.addAttribute("status", status.name());
        model.addAttribute("type", type == null ? "" : type.name());
        model.addAttribute("basePath", basePath);
        model.addAttribute("returnTo", basePath + "?status=" + status.name());
        model.addAttribute("currentUser", counselor);
        return "counselor/reviews";
    }

    private void ensureCounselorDataScope(AppUser counselor, Declaration row) {
        String scopedClass = clean(counselor.getClassName());
        if (StringUtils.hasText(scopedClass) && scopedClass.equals(row.getClassName()) == false) {
            throw new IllegalArgumentException("无权审核其他班级申报");
        }
    }

    private String safeReturn(String returnTo) {
        if (returnTo != null && returnTo.startsWith("/")) {
            return returnTo;
        }
        return "/counselor/reviews?status=PENDING";
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
