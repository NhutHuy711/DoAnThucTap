//package com.freshcart.chatbot;
//
//import com.freshcart.product.ProductEmbeddingInitService;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//
//public class EmbeddingInitializer implements CommandLineRunner {
//
//    private final ProductEmbeddingInitService embeddingInitService;
//
//    public EmbeddingInitializer(ProductEmbeddingInitService embeddingInitService) {
//        this.embeddingInitService = embeddingInitService;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        System.out.println("🚀 Generating embeddings for all products...");
//        embeddingInitService.generateEmbeddingsForAllProducts();
//    }
//}
//
