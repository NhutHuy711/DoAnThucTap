package com.freshcart.product;

import com.freshcart.common.entity.product.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductDetailRepository extends JpaRepository<ProductDetail, Long> {

    @Query("SELECT d FROM ProductDetail d WHERE d.product.id = ?1")
    List<ProductDetail> findByProductId(Integer productId);
}
