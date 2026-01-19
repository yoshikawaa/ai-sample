package io.github.yoshikawaa.example.ai_sample.repository;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import io.github.yoshikawaa.example.ai_sample.model.Customer;

@Mapper
public interface CustomerRepository {
    @Select("SELECT * FROM customer")
    List<Customer> findAll();

    @Select("SELECT * FROM customer WHERE email = #{email}")
    Optional<Customer> findByEmail(String email);

    @Insert("""
        INSERT INTO customer (email, password, name, registration_date, birth_date, phone_number, address)
        VALUES (#{email}, #{password}, #{name}, #{registrationDate}, #{birthDate}, #{phoneNumber}, #{address})
    """)
    void save(Customer customer);

    @Update("""
        UPDATE customer
        SET password = #{password}
        WHERE email = #{email}
    """)
    void updatePassword(@Param("email") String email, @Param("password") String password);

    @Update("""
        UPDATE customer
        SET name = #{name}, birth_date = #{birthDate}, phone_number = #{phoneNumber}, address = #{address}
        WHERE email = #{email}
    """)
    void updateCustomerInfo(Customer customer);
}

