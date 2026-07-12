package com.hust.orderservice.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.event.OrderPaidEvent;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.orderservice.constant.OrderStatus;
import com.hust.orderservice.constant.PaymentStatus;
import com.hust.orderservice.dto.event.OrderPaidInternalEvent;
import com.hust.orderservice.dto.response.PaymentResponse;
import com.hust.orderservice.entity.Order;
import com.hust.orderservice.entity.OrderItem;
import com.hust.orderservice.entity.Payment;
import com.hust.orderservice.mapper.PaymentMapper;
import com.hust.orderservice.repository.OrderRepository;
import com.hust.orderservice.repository.PaymentRepository;
import com.hust.orderservice.service.PaymentService;
import com.hust.orderservice.service.VNPAYService;
import com.hust.orderservice.utils.VNPAYUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final VNPAYService vnpayService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.hust.commonlibrary.service.RedisService redisService;

    @Override
    @Transactional
    public Map<String, String> processVNPAYIPN(Map<String, String> params) {
        Map<String, String> response = new HashMap<>();

        try {
            // 1. Kiểm tra chữ ký (Checksum)
            if (!vnpayService.verifyCallback(params)) {
                log.error("IPN Error: Invalid Checksum");
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return response;
            }

            // 2. Giải mã vnp_TxnRef để lấy lại OrderId
            String vnp_TxnRef = params.get("vnp_TxnRef");
            String orderId = VNPAYUtils.decodeOrderId(vnp_TxnRef);

            BigDecimal vnp_Amount = new BigDecimal(params.get("vnp_Amount")).divide(BigDecimal.valueOf(100));

            // 3. Kiểm tra đơn hàng có tồn tại không
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.error("IPN Error: Order {} not found", orderId);
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return response;
            }

            // 4. Kiểm tra số tiền có khớp không
            if (order.getTotalPrice().compareTo(vnp_Amount) != 0) {
                log.error("IPN Error: Invalid amount for Order {}. Expected {}, got {}", orderId, order.getTotalPrice(), vnp_Amount);
                response.put("RspCode", "04");
                response.put("Message", "Invalid amount");
                return response;
            }

            // 5. Kiểm tra trạng thái đơn hàng (Đảm bảo không xử lý lại đơn đã xong)
            if (order.getStatus() != OrderStatus.PENDING) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }

            // 6. Xử lý kết quả thanh toán từ VNPay
            String responseCode = params.get("vnp_ResponseCode");
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(new Payment());
            payment.setOrder(order);
            payment.setTransactionId(params.get("vnp_TransactionNo"));
            payment.setMethod("VNPAY");
            payment.setAmount(vnp_Amount);
            payment.setBankCode(params.get("vnp_BankCode"));
            payment.setBankTranNo(params.get("vnp_BankTranNo"));
            payment.setCardType(params.get("vnp_CardType"));
            payment.setOrderInfo(params.get("vnp_OrderInfo"));
            payment.setVnpTxnRef(vnp_TxnRef);
            payment.setPayDate(Instant.now());

            if ("00".equals(responseCode)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                order.setStatus(OrderStatus.PAYMENT_SUCCESS);
                order.setTransactionMethod("VNPAY");
                order.setTransactionNo(params.get("vnp_TransactionNo"));
                log.info("IPN Success: Order {} marked as PAYMENT_SUCCESS", orderId);

                publishOrderPaidEvent(order);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                order.setStatus(OrderStatus.CANCELLED);
                order.setTransactionMethod("VNPAY");
                order.setTransactionNo(params.get("vnp_TransactionNo"));
                log.warn("IPN Failed: Order {} marked as CANCELLED (ResponseCode: {})", orderId, responseCode);
            }

            paymentRepository.save(payment);
            orderRepository.save(order);

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");

        } catch (Exception e) {
            log.error("IPN Unexpected Error for params {}: ", params, e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }

        return response;
    }

    private void publishOrderPaidEvent(Order order) {
        log.info("Publishing OrderPaidInternalEvent for Order ID: {}", order.getId());
        try {
            List<String> courseIds = order.getItems().stream()
                    .map(OrderItem::getCourseId)
                    .toList();

            OrderPaidEvent event = OrderPaidEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .courseIds(courseIds)
                    .build();

            eventPublisher.publishEvent(new OrderPaidInternalEvent(this, event));

        } catch (Exception e) {
            log.error("Failed to publish OrderPaidInternalEvent for Order {}: {}", order.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public String processVNPAYReturn(Map<String, String> params) {
        if (!vnpayService.verifyCallback(params)) {
            log.error("Return Error: Invalid Checksum");
            return "signature_invalid";
        }

        String responseCode = params.get("vnp_ResponseCode");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String orderId = VNPAYUtils.decodeOrderId(vnp_TxnRef);

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.error("Return Error: Order {} not found", orderId);
            return "failed";
        }

        // Nếu đơn hàng chưa được xử lý thanh toán (do IPN chưa nhận được trên môi trường localhost)
        if (order.getStatus() == OrderStatus.PENDING) {
            BigDecimal vnp_Amount = new BigDecimal(params.get("vnp_Amount")).divide(BigDecimal.valueOf(100));

            Payment payment = paymentRepository.findByOrderId(orderId).orElse(new Payment());
            payment.setOrder(order);
            payment.setTransactionId(params.get("vnp_TransactionNo"));
            payment.setMethod("VNPAY");
            payment.setAmount(vnp_Amount);
            payment.setBankCode(params.get("vnp_BankCode"));
            payment.setBankTranNo(params.get("vnp_BankTranNo"));
            payment.setCardType(params.get("vnp_CardType"));
            payment.setOrderInfo(params.get("vnp_OrderInfo"));
            payment.setVnpTxnRef(vnp_TxnRef);
            payment.setPayDate(Instant.now());

            if ("00".equals(responseCode)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                order.setStatus(OrderStatus.PAYMENT_SUCCESS);
                order.setTransactionMethod("VNPAY");
                order.setTransactionNo(params.get("vnp_TransactionNo"));
                log.info("Return Success: Order {} marked as PAYMENT_SUCCESS via redirect", orderId);

                publishOrderPaidEvent(order);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                order.setStatus(OrderStatus.CANCELLED);
                order.setTransactionMethod("VNPAY");
                order.setTransactionNo(params.get("vnp_TransactionNo"));
                log.warn("Return Failed: Order {} marked as CANCELLED via redirect (ResponseCode: {})", orderId, responseCode);
            }

            paymentRepository.save(payment);
            orderRepository.save(order);
        } else {
            log.info("Return: Order {} already processed with status: {}", orderId, order.getStatus());
        }

        return "00".equals(responseCode) ? "success" : "failed";
    }

    private void populateUserProfile(PaymentResponse response, Payment payment) {
        if (payment == null || payment.getOrder() == null) return;
        try {
            String userId = payment.getOrder().getUserId();
            if (userId == null) return;
            String key = com.hust.commonlibrary.constants.RedisPrefixConstants.getSharedUserProfileKey(userId);
            com.hust.commonlibrary.dto.UserSharedProfile profile = redisService.get(key, com.hust.commonlibrary.dto.UserSharedProfile.class);
            if (profile != null) {
                response.setUser(profile);
            }
        } catch (Exception e) {
            log.error("Failed to load user shared profile from Redis for payment {}: {}", response.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ListResponse<PaymentResponse> getAllPaymentsForAdmin(Pageable pageable) {
        log.info("Fetching paginated transactions for Admin view");
        Page<Payment> page = paymentRepository.findAll(pageable);
        List<PaymentResponse> content = page.stream()
                .map(payment -> {
                    PaymentResponse resp = paymentMapper.entityToResponse(payment);
                    populateUserProfile(resp, payment);
                    return resp;
                })
                .toList();
        return ListResponse.of(content, page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String id) {
        log.info("Fetching transaction detail for Payment ID: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        return paymentMapper.entityToResponse(payment);
    }
}
