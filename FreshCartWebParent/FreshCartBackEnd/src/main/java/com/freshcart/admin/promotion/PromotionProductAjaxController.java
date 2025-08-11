package com.freshcart.admin.promotion;

import com.freshcart.admin.product.ProductRepository;
import com.freshcart.common.entity.product.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/promotions/products")
public class PromotionProductAjaxController {

    @Autowired
    private ProductRepository productRepo;

    @GetMapping("/search")
    public List<Select2Option> search(@RequestParam String term,
                                      @RequestParam(defaultValue = "1") int page) {

        String kw = term == null ? "" : term.trim();
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), 20, Sort.by("name").ascending());

        // tìm theo tên
        Page<Product> pageByName = productRepo.findByNameContainingIgnoreCase(kw, pageable);
        List<Product> results = new ArrayList<>(pageByName.getContent());

        // nếu người dùng gõ toàn số -> thử match ID và ưu tiên lên đầu
        if (kw.matches("\\d+")) {
            productRepo.findById(Integer.parseInt(kw)).ifPresent(p -> {
                boolean exists = results.stream().anyMatch(x -> x.getId().equals(p.getId()));
                if (!exists) results.add(0, p);
            });
        }

        return results.stream()
                .map(p -> new Select2Option(p.getId(), p.getId() + " - " + p.getName()))
                .collect(Collectors.toList());
    }

    // POJO thay cho record (Java 8/11 OK)
    public static class Select2Option {
        private Integer id;
        private String text;

        public Select2Option() {}
        public Select2Option(Integer id, String text) { this.id = id; this.text = text; }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}
