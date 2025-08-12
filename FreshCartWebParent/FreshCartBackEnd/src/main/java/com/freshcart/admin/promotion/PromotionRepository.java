package com.freshcart.admin.promotion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.freshcart.common.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.freshcart.admin.paging.SearchRepository;
import org.springframework.data.repository.query.Param;

public interface PromotionRepository extends SearchRepository<Promotion, Integer> {

    @Query("SELECT p FROM Promotion p WHERE p.name LIKE %?1% "
            + "OR CAST(p.percentOff AS string) LIKE %?1% "
            + "OR FUNCTION('DATE_FORMAT', p.startAt, '%Y-%m-%d') LIKE %?1% "
            + "OR FUNCTION('DATE_FORMAT', p.endAt, '%Y-%m-%d') LIKE %?1%")
    public Page<Promotion> findAll(String keyword, Pageable pageable);

    public List<Promotion> findAll();

    @Query("UPDATE Promotion p SET p.enabled = ?2 WHERE p.id = ?1")
    @Modifying
    public void updateEnabledStatus(Integer id, boolean enabled);

    @Query("SELECT p FROM Promotion p LEFT JOIN FETCH p.products WHERE p.id = ?1")
    Optional<Promotion> findByIdWithProducts(Integer id);

    @Query("SELECT pr FROM Promotion pr JOIN pr.products prod "
            + "WHERE prod.id = :productId "
            + "AND pr.enabled = true "
            + "AND (:excludeId IS NULL OR pr.id <> :excludeId) "
            + "AND pr.startAt <= :endAt "
            + "AND pr.endAt   >= :startAt")
    List<Promotion> findOverlapsByProduct(@Param("productId") Integer productId,
                                          @Param("startAt") LocalDateTime startAt,
                                          @Param("endAt") LocalDateTime endAt,
                                          @Param("excludeId") Integer excludeId);

}
