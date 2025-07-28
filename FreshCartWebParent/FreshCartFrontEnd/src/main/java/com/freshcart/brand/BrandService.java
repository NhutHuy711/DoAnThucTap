package com.freshcart.brand;

import java.util.List;

import com.freshcart.common.exception.BrandNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.freshcart.common.entity.Brand;
import com.freshcart.common.entity.Category;

@Service
public class BrandService {
    @Autowired
    private BrandRepository brandRepository;

    public List<Brand> listAll() {
        return brandRepository.findAllOrderByNameAsc();
    }

    public List<Brand> listByCategory(Category category) {
        String categoryIDMatch = "-" + category.getId() + "-";
        return brandRepository.findByCategory(category.getId(), categoryIDMatch);
    }

    public Brand getBrand(String name) throws BrandNotFoundException {
        Brand brand = brandRepository.findByNameIgnoreCase(name);
        if (brand == null) {
            throw new BrandNotFoundException("Could not find any brands with name " + name);
        }

        return brand;
    }
} 