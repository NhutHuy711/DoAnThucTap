package com.freshcart.order;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.freshcart.ControllerHelper;
import com.freshcart.category.CategoryService;
import com.freshcart.common.entity.Category;
import com.freshcart.common.entity.Customer;
import com.freshcart.common.entity.order.Order;
import com.freshcart.common.entity.order.OrderDetail;
import com.freshcart.common.entity.product.Product;
import com.freshcart.review.ReviewService;

@Controller
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private ControllerHelper controllerHelper;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private CategoryService categoryService;

    @GetMapping("/orders")
    public String listFirstPage(Model model, HttpServletRequest request) {
        return listOrdersByPage(model, request, 1, "orderTime", "desc", null);
    }

    @GetMapping("/orders/page/{pageNum}")
    public String listOrdersByPage(Model model, HttpServletRequest request,
                                   @PathVariable(name = "pageNum") int pageNum,
                                   String sortField, String sortDir, String keyword
    ) {
        Customer customer = controllerHelper.getAuthenticatedCustomer(request);

        Page<Order> page = orderService.listForCustomerByPage(customer, pageNum, sortField, sortDir, keyword);
        List<Order> listOrders = page.getContent();
        List<Category> listCategories = categoryService.listHierarchicalCategories();
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("currentPage", pageNum);
        model.addAttribute("listOrders", listOrders);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("keyword", keyword);
        model.addAttribute("moduleURL", "/orders");
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("listCategories", listCategories);
        long startCount = (pageNum - 1) * OrderService.ORDERS_PER_PAGE + 1;
        model.addAttribute("startCount", startCount);

        long endCount = startCount + OrderService.ORDERS_PER_PAGE - 1;
        if (endCount > page.getTotalElements()) {
            endCount = page.getTotalElements();
        }

        model.addAttribute("endCount", endCount);

        return "orders/orders_customer";
    }

    @GetMapping("/orders/detail/{id}")
    public String viewOrderDetails(Model model,
                                   @PathVariable(name = "id") Integer id, HttpServletRequest request) {
        Customer customer = controllerHelper.getAuthenticatedCustomer(request);
        Order order = orderService.getOrder(id, customer);

        setProductReviewableStatus(customer, order);

        model.addAttribute("order", order);

        return "orders/order_details_modal";
    }

    private void setProductReviewableStatus(Customer customer, Order order) {
        Iterator<OrderDetail> iterator = order.getOrderDetails().iterator();

        while (iterator.hasNext()) {
            OrderDetail orderDetail = iterator.next();
            Product product = orderDetail.getProduct();
            Integer productId = product.getId();

            boolean didCustomerReviewProduct = reviewService.didCustomerReviewProduct(customer, productId);
            product.setReviewedByCustomer(didCustomerReviewProduct);

            if (!didCustomerReviewProduct) {
                boolean canCustomerReviewProduct = reviewService.canCustomerReviewProduct(customer, productId);
                product.setCustomerCanReview(canCustomerReviewProduct);
            }

        }
    }
}
