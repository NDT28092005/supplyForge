package com.supplyforge.ai.domain.repository;

import com.supplyforge.ai.domain.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByUserId(String userId);
}
