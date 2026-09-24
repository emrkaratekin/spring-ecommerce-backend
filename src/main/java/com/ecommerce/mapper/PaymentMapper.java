package com.ecommerce.mapper;

import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment, String orderNumber, String clientSecret) {
        return new PaymentResponse(
                orderNumber,
                payment.getStripePaymentIntentId(),
                clientSecret,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getFailureReason()
        );
    }
}
