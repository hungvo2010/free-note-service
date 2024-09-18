package com.freenote.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
public interface ProductRepository extends JpaRepository<Product, ProductIdentifier> {

}
