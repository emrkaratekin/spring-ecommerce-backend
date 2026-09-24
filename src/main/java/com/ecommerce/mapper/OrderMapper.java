package com.ecommerce.mapper;

import com.ecommerce.dto.request.AddressRequest;
import com.ecommerce.dto.response.AddressResponse;
import com.ecommerce.dto.response.OrderItemResponse;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.OrderSummaryResponse;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public Address toAddress(AddressRequest request) {
        return Address.builder()
                .recipientName(request.recipientName().trim())
                .phoneNumber(request.phoneNumber().trim())
                .street(request.street().trim())
                .city(request.city().trim())
                .postalCode(request.postalCode().trim())
                .country(request.country().trim())
                .build();
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                toAddressResponse(order.getShippingAddress()),
                order.getItems().stream().map(this::toItemResponse).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }

    private AddressResponse toAddressResponse(Address address) {
        return new AddressResponse(
                address.getRecipientName(),
                address.getPhoneNumber(),
                address.getStreet(),
                address.getCity(),
                address.getPostalCode(),
                address.getCountry()
        );
    }
}
