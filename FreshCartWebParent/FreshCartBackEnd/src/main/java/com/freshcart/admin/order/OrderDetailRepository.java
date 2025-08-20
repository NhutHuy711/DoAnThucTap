package com.freshcart.admin.order;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.freshcart.common.entity.order.OrderDetail;
import org.springframework.data.repository.query.Param;

public interface OrderDetailRepository extends CrudRepository<OrderDetail, Integer> {

    @Query("""
        SELECT NEW com.freshcart.common.entity.order.OrderDetail(
                d.product.category.name, d.quantity, d.productCost, d.shippingCost, d.subtotal)
        FROM OrderDetail d
        WHERE d.order.orderTime BETWEEN :startTime AND :endTime
            AND d.order.status NOT IN (
                com.freshcart.common.entity.order.OrderStatus.CANCELLED,
                com.freshcart.common.entity.order.OrderStatus.RETURNED,
                com.freshcart.common.entity.order.OrderStatus.REFUNDED
            )
            AND (
                (d.order.paymentMethod = com.freshcart.common.entity.order.PaymentMethod.COD
                    AND d.order.status = com.freshcart.common.entity.order.OrderStatus.DELIVERED)
                OR
                (d.order.paymentMethod = com.freshcart.common.entity.order.PaymentMethod.PAYPAL)
          )
        """)
    List<OrderDetail> findWithCategoryAndTimeBetween(@Param("startTime") Date startTime,
                                                     @Param("endTime") Date endTime);



    @Query("""
        SELECT NEW com.freshcart.common.entity.order.OrderDetail(
            d.quantity, d.product.name, d.productCost, d.shippingCost, d.subtotal)
        FROM OrderDetail d
        WHERE d.order.orderTime BETWEEN :startTime AND :endTime
            AND d.order.status NOT IN (
                 com.freshcart.common.entity.order.OrderStatus.CANCELLED,
                 com.freshcart.common.entity.order.OrderStatus.RETURNED,
                 com.freshcart.common.entity.order.OrderStatus.REFUNDED
            )
            AND (
                 (d.order.paymentMethod = com.freshcart.common.entity.order.PaymentMethod.COD
                      AND d.order.status = com.freshcart.common.entity.order.OrderStatus.DELIVERED)
                 OR
                 (d.order.paymentMethod = com.freshcart.common.entity.order.PaymentMethod.PAYPAL)
          )
        """)
    List<OrderDetail> findWithProductAndTimeBetween(@Param("startTime") Date startTime,
                                                    @Param("endTime") Date endTime);

}
