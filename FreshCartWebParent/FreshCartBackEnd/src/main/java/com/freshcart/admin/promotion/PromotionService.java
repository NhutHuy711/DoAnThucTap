package com.freshcart.admin.promotion;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.transaction.Transactional;

import com.freshcart.admin.product.ProductRepository;
import com.freshcart.common.entity.Promotion;
import com.freshcart.common.entity.Review;
import com.freshcart.common.entity.product.Product;
import com.freshcart.common.exception.ReviewNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.freshcart.admin.paging.PagingAndSortingHelper;
import com.freshcart.common.exception.PromotionNotFoundException;

@Service
@Transactional
public class PromotionService {

    public static final int PROMOTIONS_PER_PAGE = 5;

    @Autowired
    private PromotionRepository promotionRepo;

    @Autowired
    private ProductRepository productRepo;

    public void listByPage(int pageNum, PagingAndSortingHelper helper) {
        helper.listEntities(pageNum, PROMOTIONS_PER_PAGE, promotionRepo);
    }

    public Promotion get(Integer id) throws PromotionNotFoundException {
        try {
            return promotionRepo.findById(id).get();
        } catch (NoSuchElementException ex) {
            throw new PromotionNotFoundException("Could not find any promotions with ID " + id);
        }
    }

    public void save(Promotion p) {
        // 1) Map productIds -> Set<Product>
        List<Integer> ids = p.getProductIds();
        Set<Product> set = (ids == null || ids.isEmpty())
                ? new HashSet<>()
                : new HashSet<>(productRepo.findByIdIn(ids));
        p.setProducts(set);

        // 2) Validate thời gian
        if (p.getStartAt() != null && p.getEndAt() != null && p.getEndAt().isBefore(p.getStartAt())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        // 3) Check trùng thời gian (mỗi sản phẩm chỉ có 1 promotion hiệu lực trong cùng thời gian)
        for (Product prod : set) {
            List<Promotion> overlaps = promotionRepo.findOverlapsByProduct(
                    prod.getId(), p.getStartAt(), p.getEndAt(), p.getId());
            if (!overlaps.isEmpty()) {
                throw new IllegalArgumentException("Product ID " + prod.getId()
                        + " already has an active promotion overlapping this period.");
            }
        }

        // 4) Save
        promotionRepo.save(p);
    }

    public void delete(Integer id) throws PromotionNotFoundException {
        if (!promotionRepo.existsById(id)) {
            throw new PromotionNotFoundException("Could not find any promotions with ID " + id);
        }
        promotionRepo.deleteById(id);
    }

    public void updatePromotionEnabledStatus(Integer id, boolean enabled) {
        promotionRepo.updateEnabledStatus(id, enabled);
    }
}
