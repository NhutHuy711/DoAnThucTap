package com.freshcart;

import com.freshcart.brand.BrandService;
import com.freshcart.category.CategoryService;
import com.freshcart.common.entity.Brand;
import com.freshcart.common.entity.Category;
import com.freshcart.product.ProductService;
import com.freshcart.common.entity.product.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainController {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductService productService;
    @Autowired
    private BrandService brandService;

    @GetMapping("")
    public String viewHomePage(Model model) {
        List<Category> listCategories = categoryService.listHierarchicalCategories();
        List<Brand> listBrands = brandService.listAll();
        Map<Brand, List<Category>> brandCategoriesMap = new LinkedHashMap<>();

        for (Brand brand : listBrands) {
            List<Category> categories = categoryService.listCategoriesByBrand(brand.getId());
            brandCategoriesMap.put(brand, categories);
        }
        
        // Thêm danh sách sản phẩm cho trang chủ
        List<Product> listNewProducts = productService.listNewProducts();
        List<Product> listSpecialOffers = productService.listSpecialOffers();
        List<Product> listBestSellers = productService.listBestSellingProducts(10); // Lấy top 10 sản phẩm bán chạy

        model.addAttribute("brandCategoriesMap", brandCategoriesMap);
        model.addAttribute("listCategories", listCategories);
        model.addAttribute("listBrands", listBrands);
        model.addAttribute("listNewProducts", listNewProducts);
        model.addAttribute("listSpecialOffers", listSpecialOffers);
        model.addAttribute("listBestSellers", listBestSellers);
        
        return "index";
    }

    @GetMapping("/login")
    public String viewLoginPage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "login";
        }

        return "redirect:/";
    }

}
