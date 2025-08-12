package com.freshcart.common.entity;

import com.freshcart.common.entity.IdBasedEntity;
import com.freshcart.common.entity.product.Product;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "promotions")
public class Promotion extends IdBasedEntity {

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "percent_off", nullable = false)
    private float percentOff;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private boolean enabled = true;

    // Liên kết nhiều–nhiều với Product thông qua bảng promotion_products
    @ManyToMany
    @JoinTable(
            name = "promotion_products",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> products = new HashSet<>();

    @Transient
    private List<Integer> productIds = new ArrayList<>();

    public Promotion() {}

    public Promotion(String name, float percentOff, LocalDateTime startAt, LocalDateTime endAt) {
        this.name = name;
        this.percentOff = percentOff;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Product> getProducts() {
        return products;
    }

    public void setProducts(Set<Product> products) {
        this.products = products;
    }

    public float getPercentOff() {
        return percentOff;
    }

    public void setPercentOff(float percentOff) {
        this.percentOff = percentOff;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public List<Integer> getProductIds() { return productIds; }
    public void setProductIds(List<Integer> productIds) { this.productIds = productIds; }


}
