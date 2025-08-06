package com.freshcart.common.entity.product;

import javax.persistence.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshcart.common.entity.IdBasedEntity;

import java.io.IOException;
import java.util.List;

@Entity
@Table(name = "product_embedding")
public class ProductEmbedding extends IdBasedEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, referencedColumnName = "id")
    private Product product;

    @Lob
    @Column(columnDefinition = "JSON", nullable = false)
    private String embedding; // lưu embedding dạng JSON string

    // ---- Convenience methods ----
    public List<Double> getEmbeddingAsList() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(embedding, new TypeReference<List<Double>>() {});
    }

    public void setEmbeddingFromList(List<Double> vector) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.embedding = mapper.writeValueAsString(vector);
    }

    // ---- Getter/Setter ----
    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }
}
