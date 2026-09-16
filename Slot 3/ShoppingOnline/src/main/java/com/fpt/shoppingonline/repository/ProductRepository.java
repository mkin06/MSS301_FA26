package com.fpt.shoppingonline.repository;

import com.fpt.shoppingonline.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
