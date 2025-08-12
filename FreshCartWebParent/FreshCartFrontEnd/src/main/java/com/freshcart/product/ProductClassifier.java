package com.freshcart.product;

import com.freshcart.common.entity.product.Product;
import com.freshcart.common.entity.product.ProductDetail;

import java.util.List;

public class ProductClassifier {
    public static String classify(Product product, List<ProductDetail> details) {
        int ram = extractRam(details);
        String gpu = extractGpu(details).toLowerCase();
        float price = product.getPrice();
        float weight = product.getWeight();

        // Gaming
        if ((gpu.contains("rtx") || gpu.contains("gtx") || gpu.contains("radeon rx")) && ram >= 16) {
            return "Gaming";
        }

        // Đồ họa
        if (gpu.contains("quadro") || gpu.contains("radeon pro") ||
                (gpu.contains("rtx") && ram >= 16 && price >= 25_000_000f)) {
            return "Đồ họa";
        }

        // Văn phòng
        if ((gpu.contains("iris") || gpu.contains("uhd") || gpu.contains("apple") || gpu.contains("vega"))
                && ram >= 8 && ram <= 16 && weight <= 1.6f) {
            return "Văn phòng";
        }

        // Sinh viên
        if (price <= 15_000_000f &&
                (gpu.contains("iris") || gpu.contains("apple") || gpu.contains("uhd")) &&
                ram >= 8) {
            return "Sinh viên";
        }

        return "Khác";
    }

    private static int extractRam(List<ProductDetail> details) {
        for (ProductDetail d : details) {
            String name = d.getName().toLowerCase();
            String value = d.getValue().toLowerCase();
            if (name.contains("ram") || name.contains("dung lượng ram")) {
                String digits = value.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    try {
                        return Integer.parseInt(digits);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return 0;
    }

    private static String extractGpu(List<ProductDetail> details) {
        for (ProductDetail d : details) {
            String name = d.getName().toLowerCase();
            String value = d.getValue().toLowerCase();
            if (name.contains("card") || name.contains("gpu") || value.contains("gpu")) {
                return value;
            }
        }
        return "";
    }
}
