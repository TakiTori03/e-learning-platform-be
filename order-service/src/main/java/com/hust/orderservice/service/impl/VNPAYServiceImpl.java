package com.hust.orderservice.service.impl;

import com.hust.orderservice.service.VNPAYService;
import com.hust.orderservice.utils.VNPAYUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class VNPAYServiceImpl implements VNPAYService {

    @Value("${payment.vnpay.tmn-code}")
    private String tmnCode;

    @Value("${payment.vnpay.hash-secret}")
    private String hashSecret;

    @Value("${payment.vnpay.url}")
    private String vnpPayUrl;

    @Value("${payment.vnpay.return-url}")
    private String returnUrl;

    @Override
    public String createPaymentUrl(String orderId, long amount, String orderInfo, String ipAddress) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        
        // Nén UUID 36 ký tự thành 22 ký tự để đảm bảo luôn dưới giới hạn 30 của VNPay
        String vnp_TxnRef = VNPAYUtils.encodeOrderId(orderId);

        // Chuẩn hóa IP (VNPay không hỗ trợ IPv6 localhost hoặc danh sách nhiều IP do proxy)
        String vnp_IpAddr = ipAddress;
        if (vnp_IpAddr != null && vnp_IpAddr.contains(",")) {
            vnp_IpAddr = vnp_IpAddr.split(",")[0].trim();
        }
        if (vnp_IpAddr == null || vnp_IpAddr.equals("0:0:0:0:0:0:0:1") || vnp_IpAddr.equals("[0:0:0:0:0:0:0:1]")) {
            vnp_IpAddr = "127.0.0.1";
        }

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", tmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo != null ? orderInfo : "Payment for order " + orderId);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // 1. Tính toán chữ ký trên dữ liệu RAW (chuẩn 2.1.0)
        log.info("VNPAY Creation - tmnCode: {}, hashSecret (masked): {}", 
                 tmnCode, 
                 (hashSecret != null && hashSecret.length() > 6) ? hashSecret.substring(0, 3) + "***" + hashSecret.substring(hashSecret.length() - 3) : "null");
        String vnp_SecureHash = VNPAYUtils.hashAllFields(hashSecret, vnp_Params);
        
        // 2. Tạo Query String với dữ liệu đã ENCODE
        String queryUrl = VNPAYUtils.buildQueryUrl(vnp_Params);

        String redirectUrl = vnpPayUrl + "?" + queryUrl + "&vnp_SecureHash=" + vnp_SecureHash;
        log.info("Generated VNPAY Redirect URL: {}", redirectUrl);

        return redirectUrl;
    }

    @Override
    public Boolean verifyCallback(Map<String, String> queryParams) {
        String vnp_SecureHash = queryParams.get("vnp_SecureHash");
        if (vnp_SecureHash == null) {
            return false;
        }
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && key.startsWith("vnp_") && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
                fields.put(key, value);
            }
        }

        String checkSum = VNPAYUtils.hashAllFields(hashSecret, fields);
        return checkSum.equalsIgnoreCase(vnp_SecureHash);
    }
}
