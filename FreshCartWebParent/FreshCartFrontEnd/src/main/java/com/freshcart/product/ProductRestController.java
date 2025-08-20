//package com.freshcart.product;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.*;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/products")
//public class ProductRestController {
//
//    @Autowired
//    private ProductService productService;
//
//    @Autowired
//    private EmbeddingSearchService embeddingSearchService;
//
//    @GetMapping("/price")
//    public Map<String, Object> getPrice(@RequestParam String name) {
//        Float price = productService.getPrice(name);
//        return Map.of("name", name, "price", price != null ? price : -1);
//    }
//
//    @GetMapping("/recommend")
//    public Map<String, Object> recommendPhones(@RequestParam(required = false) Integer brandId,
//                                               @RequestParam(required = false) Float budget,
//                                               @RequestParam String query) {
//
//        List<ProductDTO> filtered = productService.getFilteredProducts(brandId, budget);
//
//        // Convert sang Map nếu cần
//        List<Map<String, Object>> productsAsMap = filtered.stream().map(p -> {
//            Map<String, Object> m = new HashMap<>();
//            m.put("id", p.getId());
//            m.put("name", p.getName());
//            m.put("price", p.getPrice());
//            m.put("alias", p.getAlias());
//            m.put("link", p.getLink());
//            m.put("description", p.getDescription());
//            m.put("specs", p.getSpecs());
//            return m;
//        }).collect(Collectors.toList());
//
//        List<Map<String, Object>> results = embeddingSearchService.searchTopK(query, productsAsMap, 5);
//
//        return Map.of("results", results);
//    }
//}
