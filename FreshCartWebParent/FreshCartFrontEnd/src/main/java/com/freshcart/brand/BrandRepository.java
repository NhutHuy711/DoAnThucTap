package com.freshcart.brand;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.freshcart.common.entity.Brand;
import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Integer> {
    @Query("SELECT DISTINCT b FROM Brand b " +
           "JOIN Product p ON p.brand.id = b.id " +
           "WHERE b.enabled = true " +
           "AND (p.category.id = :categoryId OR p.category.allParentIDs LIKE %:categoryMatch%) " +
           "ORDER BY b.name ASC")
    public List<Brand> findByCategory(Integer categoryId, String categoryMatch);

    @Query("SELECT b FROM Brand b WHERE b.enabled = true ORDER BY b.name ASC")
    public List<Brand> findAllOrderByNameAsc();

    @Query("SELECT b FROM Brand b WHERE b.enabled = true AND b.name = ?1")
    public Brand findByNameEnabled(String name);

    @Query("SELECT b FROM Brand b WHERE LOWER(b.name) = LOWER(?1) AND b.enabled = true")
    public Brand findByNameIgnoreCase(String name);

}