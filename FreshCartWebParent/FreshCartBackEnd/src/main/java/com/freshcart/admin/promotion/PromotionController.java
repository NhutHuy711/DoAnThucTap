package com.freshcart.admin.promotion;

import com.freshcart.admin.MessageServiceAdmin;
import com.freshcart.admin.paging.PagingAndSortingHelper;
import com.freshcart.admin.paging.PagingAndSortingParam;
import com.freshcart.admin.product.ProductService;
import com.freshcart.common.entity.Promotion;
import com.freshcart.common.exception.PromotionNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class PromotionController {
    private String defaultRedirectURL = "redirect:/promotions/page/1?sortField=id&sortDir=desc";

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private ProductService productService;
    @Autowired
    private MessageServiceAdmin messageService;

    @GetMapping("/promotions")
    public String listFirstPage(Model model) {
        return defaultRedirectURL;
    }

    @GetMapping("/promotions/page/{pageNum}")
    public String listByPage(
            @PagingAndSortingParam(listName = "listPromotions", moduleURL = "/promotions") PagingAndSortingHelper helper,
            @PathVariable(name = "pageNum") int pageNum) {

        promotionService.listByPage(pageNum, helper);

        return "promotions/promotions";
    }

    @GetMapping("/promotions/new")
    public String newPromotion(Model model) {

        model.addAttribute("promotion", new Promotion());
        model.addAttribute("allProducts", productService.findAll());
        model.addAttribute("selectedProducts", List.of());
        model.addAttribute("pageTitle", "New Promotion");
        model.addAttribute("moduleURL", "/promotions/new");

        return "promotions/promotion_form";
    }

    @PostMapping("/promotions/save")
    public String savePromotion(@ModelAttribute("promotion") Promotion promotion,
                                org.springframework.validation.BindingResult binding,
                                Model model,
                                RedirectAttributes ra) {

        if (promotion.getStartAt() != null && promotion.getEndAt() != null
                && promotion.getEndAt().isBefore(promotion.getStartAt())) {
            binding.rejectValue("endAt", "date.range", "End time must be after start time.");
        }

        if (binding.hasErrors()) {
            model.addAttribute("selectedProducts", productService.findByIds(promotion.getProductIds()));
            model.addAttribute("pageTitle", (promotion.getId() == null) ? "New Promotion" : "Edit Promotion");
            model.addAttribute("toastType", "error");
            model.addAttribute("toastMessage", "Please fix the errors below.");
            return "promotions/promotion_form";
        }

        try {
            promotionService.save(promotion);

            ra.addFlashAttribute("toastType", "success");
            ra.addFlashAttribute("toastMessage", "The promotion has been saved successfully.");
            return defaultRedirectURL;

        } catch (IllegalArgumentException ex) {
            binding.reject("promotion.overlap", ex.getMessage());

            model.addAttribute("selectedProducts", productService.findByIds(promotion.getProductIds()));
            model.addAttribute("pageTitle", (promotion.getId() == null) ? "New Promotion" : "Edit Promotion");
            model.addAttribute("toastType", "error");
            model.addAttribute("toastMessage", ex.getMessage());
            return "promotions/promotion_form";
        }
    }

    @GetMapping("/promotions/{id}/enabled/{status}")
    public String updatePromotionEnabledStatus(@PathVariable("id") Integer id,
                                              @PathVariable("status") boolean enabled, RedirectAttributes redirectAttributes) {
        promotionService.updatePromotionEnabledStatus(id, enabled);
        String status = enabled ? "enabled" : "disabled";
        String message = "The promotion ID " + id + " has been " + status;
        redirectAttributes.addFlashAttribute("message", message);

        return "redirect:/promotions";
    }

    @GetMapping("/promotions/detail/{id}")
    public String viewPromotion(@PathVariable("id") Integer id, Model model, RedirectAttributes ra) {
        try {
            Promotion promotion = promotionService.get(id);
            model.addAttribute("promotion", promotion);

            return "promotions/promotion_detail_modal";
        } catch (PromotionNotFoundException ex) {
            ra.addFlashAttribute("message", ex.getMessage());
            return defaultRedirectURL;
        }
    }
}
