package io.github.yoshikawaa.example.ai_sample.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import io.github.yoshikawaa.example.ai_sample.model.Customer;

@Mapper
public interface CustomerRepository {
    @Select("SELECT * FROM customer")
    List<Customer> findAll();

    @Insert("""
        INSERT INTO customer (email, password, name, registration_date, birth_date, phone_number, address)
        VALUES (#{email}, #{password}, #{name}, #{registrationDate}, #{birthDate}, #{phoneNumber}, #{address})
    """)
    void save(Customer customer);
}
