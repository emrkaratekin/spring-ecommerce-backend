package com.ecommerce.service.impl;

import com.ecommerce.dto.request.CreateOrderRequest;
import com.ecommerce.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.OrderSummaryResponse;
import com.ecommerce.dto.response.PageResponse;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.enums.OrderStatus;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.OrderNumberGenerator;
import com.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderNumberGenerator orderNumberGenerator;

    /**
     * Converts the user's cart into an order in a single all-or-nothing transaction:
     * validates stock, takes a price snapshot, decrements stock and clears the cart.
     * If any step fails, every change is rolled back.
     */
    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        List<Long> productIds = cartRepository.findProductIdsByUserId(userId);
        if (productIds.isEmpty()) {
            throw new BusinessException("Cannot create an order from an empty cart");
        }

        // 1) Lock the product rows BEFORE loading the cart, so the stock values we read are fresh
        //    and no other transaction can change them until we commit.
        Map<Long, Product> lockedProducts = lockProducts(productIds);

        // 2) Load the cart only after the locks are held. If the same user submitted the order twice,
        //    the second request waits here and then sees an empty cart.
        Cart cart = cartRepository.findWithItemsByUserId(userId)
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new BusinessException("Cannot create an order from an empty cart"));

        Order order = Order.builder()
                .orderNumber(orderNumberGenerator.generate())
                .user(userRepository.getReferenceById(userId))
                .status(OrderStatus.PENDING)
                .shippingAddress(orderMapper.toAddress(request.shippingAddress()))
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            if (product == null) {
                throw new BusinessException("Your cart changed while the order was being placed. Please try again.");
            }

            int quantity = cartItem.getQuantity();
            validateProductForOrder(product, quantity);

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);

            order.addItem(OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(quantity)
                    .subtotal(subtotal)
                    .build());

            product.setStockQuantity(product.getStockQuantity() - quantity);
            total = total.add(subtotal);
        }

        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));

        Order saved = orderRepository.save(order);
        cart.clearItems();

        return orderMapper.toResponse(saved);
    }

    @Override
    public PageResponse<OrderSummaryResponse> getMyOrders(Long userId, Pageable pageable) {
        Page<OrderSummaryResponse> page = orderRepository.findByUserId(userId, pageable)
                .map(orderMapper::toSummary);
        return PageResponse.from(page);
    }

    @Override
    public OrderResponse getMyOrder(Long userId, String orderNumber) {
        return orderMapper.toResponse(findUserOrderOrThrow(userId, orderNumber));
    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(Long userId, String orderNumber) {
        Order order = findUserOrderOrThrow(userId, orderNumber);
        cancelAndRestoreStock(order);
        orderRepository.flush();
        return orderMapper.toResponse(order);
    }

    @Override
    public PageResponse<OrderSummaryResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<Order> orders = (status == null)
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
        return PageResponse.from(orders.map(orderMapper::toSummary));
    }

    @Override
    public OrderResponse getOrder(String orderNumber) {
        return orderMapper.toResponse(findOrderOrThrow(orderNumber));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderNumber, UpdateOrderStatusRequest request) {
        Order order = findOrderOrThrow(orderNumber);
        OrderStatus target = request.status();

        if (target == OrderStatus.PAID) {
            throw new BusinessException("Orders are marked as PAID automatically after a successful payment");
        }

        if (target == OrderStatus.CANCELLED) {
            cancelAndRestoreStock(order);
        } else {
            if (!order.getStatus().canTransitionTo(target)) {
                throw new BusinessException("Cannot change order status from "
                        + order.getStatus() + " to " + target);
            }
            order.setStatus(target);
        }

        orderRepository.flush();
        return orderMapper.toResponse(order);
    }

    private void cancelAndRestoreStock(Order order) {
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BusinessException("Order '" + order.getOrderNumber()
                    + "' cannot be cancelled in status " + order.getStatus());
        }

        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .toList();
        Map<Long, Product> lockedProducts = lockProducts(productIds);

        for (OrderItem item : order.getItems()) {
            Product product = lockedProducts.get(item.getProduct().getId());
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
    }

    private void validateProductForOrder(Product product, int quantity) {
        if (!product.isActive()) {
            throw new BusinessException("Product '" + product.getName() + "' is no longer available");
        }
        if (product.getStockQuantity() < quantity) {
            throw new BusinessException("Insufficient stock for '" + product.getName()
                    + "': requested " + quantity + ", available " + product.getStockQuantity());
        }
    }

    private Map<Long, Product> lockProducts(Collection<Long> productIds) {
        return productRepository.findAllByIdInForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Order findUserOrderOrThrow(Long userId, String orderNumber) {
        return orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
    }

    private Order findOrderOrThrow(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
    }
}
