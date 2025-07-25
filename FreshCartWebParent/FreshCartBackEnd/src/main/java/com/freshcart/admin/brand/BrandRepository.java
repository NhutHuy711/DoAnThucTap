package com.freshcart.admin.brand;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.freshcart.admin.paging.SearchRepository;
import com.freshcart.common.entity.Brand;

public interface BrandRepository extends SearchRepository<Brand, Integer> {

    public Long countById(Integer id);

    public Brand findByName(String name);

    @Query("SELECT b FROM Brand b WHERE b.name LIKE %?1%")
    public Page<Brand> findAll(String keyword, Pageable pageable);

    @Query("SELECT NEW Brand(b.id, b.name) FROM Brand b ORDER BY b.name ASC")
    public List<Brand> findAll();

    @Query("UPDATE Brand b SET b.enabled = ?2 WHERE b.id = ?1")
    @Modifying
    public void updateEnabledStatus(Integer id, boolean enabled);
}
