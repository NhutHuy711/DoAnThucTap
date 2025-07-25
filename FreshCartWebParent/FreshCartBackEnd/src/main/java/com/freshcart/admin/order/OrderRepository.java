package com.freshcart.admin.order;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.freshcart.admin.paging.SearchRepository;
import com.freshcart.common.entity.order.Order;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends SearchRepository<Order, Integer> {
        List<Order> findTop5ByOrderByOrderTimeDesc();

    @Query("SELECT o FROM Order o WHERE CONCAT('#', o.id) LIKE %?1% OR "
            + " CONCAT(o.firstName, ' ', o.lastName) LIKE %?1% OR"
            + " o.firstName LIKE %?1% OR"
            + " o.lastName LIKE %?1% OR o.phoneNumber LIKE %?1% OR"
            + " o.addressLine1 LIKE %?1% OR o.addressLine2 LIKE %?1% OR"
            + " o.postalCode LIKE %?1% OR o.city LIKE %?1% OR"
            + " o.state LIKE %?1% OR o.country LIKE %?1% OR"
            + " o.paymentMethod LIKE %?1% OR o.status LIKE %?1% OR"
            + " o.customer.firstName LIKE %?1% OR"
            + " o.customer.lastName LIKE %?1%")
    public Page<Order> findAll(String keyword, Pageable pageable);

    public Long countById(Integer id);


    @Query("SELECT NEW com.freshcart.common.entity.order.Order(o.id, o.orderTime, o.productCost,"
            + " o.subtotal, o.shippingCost, o.total) FROM Order o WHERE"
            + " o.orderTime BETWEEN ?1 AND ?2 ORDER BY o.orderTime ASC")
    public List<Order> findByOrderTimeBetween(Date startTime, Date endTime);

    @Query("SELECT COALESCE(SUM(od.quantity), 0) FROM OrderDetail od WHERE od.order.id = :orderId AND od.product.id = :productId")
    int getProductQuantityInOrder(@Param("orderId") Integer orderId, @Param("productId") Integer productId);



}
