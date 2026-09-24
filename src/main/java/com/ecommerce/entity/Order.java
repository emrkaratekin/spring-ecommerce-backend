package com.ecommerce.entity;

import com.ecommerce.entity.enums.OrderStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "user_id"),
                @Index(name = "idx_orders_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "recipientName", column = @Column(name = "shipping_recipient_name", nullable = false, length = 100)),
            @AttributeOverride(name = "phoneNumber", column = @Column(name = "shipping_phone_number", nullable = false, length = 20)),
            @AttributeOverride(name = "street", column = @Column(name = "shipping_street", nullable = false)),
            @AttributeOverride(name = "city", column = @Column(name = "shipping_city", nullable = false, length = 100)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_postal_code", nullable = false, length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "shipping_country", nullable = false, length = 100))
    })
    private Address shippingAddress;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}