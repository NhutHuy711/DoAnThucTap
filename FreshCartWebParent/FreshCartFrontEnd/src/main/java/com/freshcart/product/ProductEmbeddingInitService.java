package com.freshcart.product;

import com.freshcart.chatbot.GeminiEmbeddingService;
import com.freshcart.common.entity.product.Product;
import com.freshcart.common.entity.product.ProductEmbedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductEmbeddingInitService {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductEmbeddingRepository embeddingRepository;

    @Autowired
    private GeminiEmbeddingService geminiEmbeddingService;

    @Transactional
    public void generateEmbeddingsForAllProducts() throws Exception {
        List<Product> products = productService.findAllEnabled();

        for (Product product : products) {
            // Skip nếu đã có embedding
            if (product.getId() != null && embeddingRepository.findByProductId(product.getId()) != null) {
                // Skip nếu đã có embedding
                continue;
            }

            String textForEmbedding = product.getName() + " " + product.getShortDescription();
            double[] vector = geminiEmbeddingService.getEmbedding(textForEmbedding);

            ProductEmbedding pe = new ProductEmbedding();
            pe.setProduct(product);
            pe.setEmbeddingFromList(
                    java.util.Arrays.stream(vector).boxed().collect(Collectors.toList())
            );
            embeddingRepository.save(pe);
            System.out.println("Saved embedding for: " + product.getName());
        }
    }
}
