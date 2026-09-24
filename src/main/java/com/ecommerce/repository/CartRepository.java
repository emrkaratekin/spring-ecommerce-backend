package com.ecommerce.repository;

import com.ecommerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Loads the cart together with its items and their products in a single query
     * to avoid the N+1 problem when building the cart response.
     */
    @Query("""
            select c from Cart c
            left join fetch c.items i
            left join fetch i.product
            where c.user.id = :userId
            """)
    Optional<Cart> findWithItemsByUserId(@Param("userId") Long userId);
}
