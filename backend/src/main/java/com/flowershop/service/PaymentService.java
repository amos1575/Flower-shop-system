package com.flowershop.service;

import com.flowershop.dto.CreatePaymentRequest;
import com.flowershop.dto.PaymentIntentResponse;
import com.flowershop.dto.PaymentResponse;
import com.flowershop.entity.Order;
import com.flowershop.entity.OrderStatus;
import com.flowershop.entity.Payment;
import com.flowershop.entity.PaymentStatus;
import com.flowershop.exception.ApiException;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.repository.OrderRepository;
import com.flowershop.repository.PaymentRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Value("${app.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.stripe.currency}")
    private String currency;

    @Transactional
    public PaymentIntentResponse createPaymentIntent(String customerEmail, CreatePaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        if (!order.getCustomer().getEmail().equalsIgnoreCase(customerEmail)) {
            throw new AccessDeniedException("You do not have permission to pay for this order");
        }

        Optional<Payment> existing = paymentRepository.findByOrderId(order.getId());
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.SUCCESS) {
            throw new ApiException(HttpStatus.CONFLICT, "Order " + order.getId() + " is already paid");
        }

        long amountInSmallestUnit = order.getTotalAmount()
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        PaymentIntent intent;
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInSmallestUnit)
                    .setCurrency(currency)
                    .putMetadata("orderId", String.valueOf(order.getId()))
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();
            intent = PaymentIntent.create(params);
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed for order {}", order.getId(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Payment provider error: " + e.getMessage());
        }

        Payment payment = existing.orElseGet(Payment::new);
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod("card");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionRef(intent.getId());
        payment = paymentRepository.save(payment);

        return PaymentIntentResponse.builder()
                .paymentId(payment.getId())
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .clientSecret(intent.getClientSecret())
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(Long orderId, String requesterEmail, boolean isAdmin) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for order: " + orderId));

        if (!isAdmin && !payment.getOrder().getCustomer().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AccessDeniedException("You do not have permission to view this payment");
        }

        return PaymentResponse.from(payment);
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }

        // Read the PaymentIntent id straight from the raw payload rather than the typed
        // deserializer: Stripe's SDK refuses to deserialize into a model object when the
        // event's api_version doesn't match the SDK's compiled version, which would otherwise
        // silently drop events after a Stripe API upgrade. We only need the id anyway.
        String paymentIntentId = extractPaymentIntentId(payload);
        if (paymentIntentId == null) {
            log.info("Ignoring webhook event {} — no payment_intent id found in payload", event.getType());
            return;
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> markPaymentStatus(paymentIntentId, PaymentStatus.SUCCESS);
            case "payment_intent.payment_failed" -> markPaymentStatus(paymentIntentId, PaymentStatus.FAILED);
            default -> log.info("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private String extractPaymentIntentId(String payload) {
        try {
            JsonObject dataObject = JsonParser.parseString(payload).getAsJsonObject()
                    .getAsJsonObject("data").getAsJsonObject("object");
            return dataObject.has("id") ? dataObject.get("id").getAsString() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void markPaymentStatus(String transactionRef, PaymentStatus status) {
        paymentRepository.findByTransactionRef(transactionRef).ifPresentOrElse(payment -> {
            payment.setStatus(status);
            if (status == PaymentStatus.SUCCESS) {
                payment.setPaidAt(LocalDateTime.now());
                Order order = payment.getOrder();
                if (order.getStatus() == OrderStatus.PENDING) {
                    order.setStatus(OrderStatus.CONFIRMED);
                    orderRepository.save(order);
                }
            }
            paymentRepository.save(payment);
        }, () -> log.warn("Received webhook for unknown transactionRef: {}", transactionRef));
    }
}
