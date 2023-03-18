package com.gmontinny.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gmontinny.models.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long>{

}

