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

    @Query("SELECT pd FROM ProductDetail pd " +
            "WHERE (?1 IS NULL OR pd.product.brand.id = ?1) " +
            "AND (?2 IS NULL OR pd.product.price <= ?2)")
    List<ProductDetail> filterProducts(Integer brand, Float budget);
}
