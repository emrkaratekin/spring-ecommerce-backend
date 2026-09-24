package com.ecommerce.service.impl;

import com.ecommerce.dto.request.AddressRequest;
import com.ecommerce.dto.request.CreateOrderRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.entity.enums.OrderStatus;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.payment.PaymentGateway;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.PaymentRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.OrderNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests: no Spring context, no database. Every dependency is a Mockito mock,
 * so only the business rules of OrderServiceImpl are tested, in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final Long USER_ID = 7L;

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderNumberGenerator orderNumberGenerator;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentGateway paymentGateway;
    @Spy private OrderMapper orderMapper = new OrderMapper();

    @InjectMocks
    private OrderServiceImpl orderService;

    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateOrderRequest(new AddressRequest(
                "Emir Karatekin", "+48 600 123 456", "ul. Marszalkowska 1", "Warszawa", "00-001", "Poland"));
    }

    @Test
    @DisplayName("An order cannot be created from an empty cart")
    void createOrder_emptyCart_throws() {
        when(cartRepository.findProductIdsByUserId(USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty cart");

        verifyNoInteractions(productRepository, orderRepository);
    }

    @Test
    @DisplayName("Insufficient stock rejects the whole order and changes nothing")
    void createOrder_insufficientStock_throwsAndKeepsStock() {
        Product product = product(1L, "Clean Code", "39.90", 1);
        Cart cart = cartWith(item(product, 2));

        when(cartRepository.findProductIdsByUserId(USER_ID)).thenReturn(List.of(1L));
        when(productRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(product));
        when(cartRepository.findWithItemsByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(orderNumberGenerator.generate()).thenReturn("ORD-TEST");

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock for 'Clean Code'");

        assertThat(product.getStockQuantity()).isEqualTo(1);
        assertThat(cart.getItems()).hasSize(1);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("A successful order takes a price snapshot, decrements stock and clears the cart")
    void createOrder_success() {
        Product cleanCode = product(1L, "Clean Code", "39.90", 5);
        Product refactoring = product(2L, "Refactoring", "45.50", 3);
        Cart cart = cartWith(item(cleanCode, 2), item(refactoring, 1));

        when(cartRepository.findProductIdsByUserId(USER_ID)).thenReturn(List.of(1L, 2L));
        when(productRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(cleanCode, refactoring));
        when(cartRepository.findWithItemsByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());
        when(orderNumberGenerator.generate()).thenReturn("ORD-TEST");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(USER_ID, request);

        assertThat(response.orderNumber()).isEqualTo("ORD-TEST");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("125.30");
        assertThat(response.items()).extracting("unitPrice")
                .usingElementComparator((a, b) -> ((BigDecimal) a).compareTo((BigDecimal) b))
                .containsExactly(new BigDecimal("39.90"), new BigDecimal("45.50"));

        assertThat(cleanCode.getStockQuantity()).isEqualTo(3);
        assertThat(refactoring.getStockQuantity()).isEqualTo(2);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Cancelling a pending order restores the stock")
    void cancelMyOrder_restoresStock() {
        Product product = product(1L, "Clean Code", "39.90", 3);
        Order order = pendingOrderWith(product, 2);

        when(orderRepository.findByOrderNumberAndUserId("ORD-TEST", USER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(product));
        when(paymentRepository.findByOrderId(any())).thenReturn(Optional.empty());

        OrderResponse response = orderService.cancelMyOrder(USER_ID, "ORD-TEST");

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStockQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("A paid order cannot be cancelled by the customer")
    void cancelMyOrder_paidOrder_throws() {
        Product product = product(1L, "Clean Code", "39.90", 3);
        Order order = pendingOrderWith(product, 2);
        order.setStatus(OrderStatus.PAID);

        when(orderRepository.findByOrderNumberAndUserId("ORD-TEST", USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(USER_ID, "ORD-TEST"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be cancelled in status PAID");

        assertThat(product.getStockQuantity()).isEqualTo(3);
    }

    // ---------------------------------------------------------------- helpers

    private static Product product(Long id, String name, String price, int stock) {
        Product product = Product.builder()
                .name(name)
                .sku("SKU-" + id)
                .price(new BigDecimal(price))
                .stockQuantity(stock)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private static CartItem item(Product product, int quantity) {
        return CartItem.builder().product(product).quantity(quantity).build();
    }

    private static Cart cartWith(CartItem... items) {
        Cart cart = new Cart();
        for (CartItem item : items) {
            cart.addItem(item);
        }
        return cart;
    }

    private static Order pendingOrderWith(Product product, int quantity) {
        Order order = Order.builder()
                .orderNumber("ORD-TEST")
                .status(OrderStatus.PENDING)
                .totalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .shippingAddress(Address.builder()
                        .recipientName("Emir Karatekin").phoneNumber("+48 600 123 456")
                        .street("ul. Marszalkowska 1").city("Warszawa").postalCode("00-001").country("Poland")
                        .build())
                .build();
        order.addItem(OrderItem.builder()
                .product(product)
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(quantity)
                .subtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .build());
        return order;
    }
}
