package com.freshcart.checkout.paypal;

import java.time.Instant;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.freshcart.setting.PaymentSettingBag;
import com.freshcart.setting.SettingService;

@Component
public class PayPalService {
    private static final String TOKEN_API   = "/v1/oauth2/token";
    private static final String GET_ORDER_API = "/v2/checkout/orders/";
    private static final String CAPTURE_API   = "/v2/checkout/orders/{id}/capture";

    @Autowired private SettingService settingService;
    private final RestTemplate rest = new RestTemplate();

    // cache token đơn giản
    private volatile String token;
    private volatile long   tokenExpMs;

    /** Optional: vẫn giữ validate như cũ */
    public boolean validateOrder(String orderId) throws PayPalApiException {
        PayPalOrderResponse r = getOrderDetails(orderId);
        return r != null && r.validate(orderId);
    }

    /** GET order details với Bearer token */
    public PayPalOrderResponse getOrderDetails(String orderId) throws PayPalApiException {
        PaymentSettingBag ps = settingService.getPaymentSettings();
        String url = ps.getURL() + GET_ORDER_API + orderId;

        HttpHeaders h = new HttpHeaders();
        h.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        h.set("Accept-Language", "en_US");
        h.setBearerAuth(getAccessToken(ps));

        try {
            ResponseEntity<PayPalOrderResponse> resp =
                    rest.exchange(url, HttpMethod.GET, new HttpEntity<>(h), PayPalOrderResponse.class);
            if (resp.getStatusCode() != HttpStatus.OK) throwExceptionForNonOKResponse(resp.getStatusCode());
            return resp.getBody();
        } catch (RestClientResponseException ex) {
            throw new PayPalApiException(
                    "PayPal GET order failed: " + ex.getRawStatusCode() + " - " + ex.getResponseBodyAsString()
            );
        }
    }

    /**
     * Capture theo chuẩn:
     * 1) GET trước: nếu COMPLETED -> trả true (đã capture).
     * 2) Nếu APPROVED -> gọi CAPTURE kèm idempotency header (PayPal-Request-Id).
     * 3) Nếu gặp 422 ORDER_ALREADY_CAPTURED -> GET lại, nếu COMPLETED -> trả true.
     */
    public boolean captureAndVerify(String orderId) throws PayPalApiException {
        PaymentSettingBag ps = settingService.getPaymentSettings();

        // 1) Pre-check
        PayPalOrderResponse before = getOrderDetails(orderId);
        if (before != null && "COMPLETED".equalsIgnoreCase(before.getStatus())) {
            return true; // đã capture trước đó
        }
        if (before == null || !"APPROVED".equalsIgnoreCase(before.getStatus())) {
            throw new PayPalApiException("Order not APPROVED: " + (before == null ? "null" : before.getStatus()));
        }

        // 2) CAPTURE (idempotent)
        String url = ps.getURL() + CAPTURE_API;
        HttpHeaders h = new HttpHeaders();
        h.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Accept-Language", "en_US");
        h.setBearerAuth(getAccessToken(ps));
        // Idempotency: cố định theo orderId để tránh double-capture khi retry
        h.set("PayPal-Request-Id", "capture-" + orderId);

        try {
            ResponseEntity<String> resp =
                    rest.exchange(url, HttpMethod.POST, new HttpEntity<>("{}", h), String.class, orderId);

            if (resp.getStatusCode() != HttpStatus.CREATED && resp.getStatusCode() != HttpStatus.OK) {
                throwExceptionForNonOKResponse(resp.getStatusCode());
            }

            String body = resp.getBody();
            return body != null && body.contains("\"status\":\"COMPLETED\"");

        } catch (RestClientResponseException ex) {
            // 3) Nếu đã capture rồi (422) → kiểm tra lại trạng thái, nếu COMPLETED coi như thành công
            String body = ex.getResponseBodyAsString();
            if (ex.getRawStatusCode() == 422 && body != null && body.contains("ORDER_ALREADY_CAPTURED")) {
                PayPalOrderResponse after = getOrderDetails(orderId);
                if (after != null && "COMPLETED".equalsIgnoreCase(after.getStatus())) {
                    return true;
                }
            }
            throw new PayPalApiException("PayPal CAPTURE failed: " + ex.getRawStatusCode() + " - " + body);
        }
    }

    /** Lấy OAuth token và cache ngắn hạn */
    private String getAccessToken(PaymentSettingBag ps) throws PayPalApiException {
        long now = Instant.now().toEpochMilli();
        if (token != null && now < tokenExpMs) return token;

        HttpHeaders h = new HttpHeaders();
        h.setBasicAuth(ps.getClientID(), ps.getClientSecret());
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        h.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        try {
            ResponseEntity<TokenResponse> resp =
                    rest.exchange(ps.getURL() + TOKEN_API, HttpMethod.POST, new HttpEntity<>(form, h), TokenResponse.class);

            if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null || resp.getBody().access_token == null) {
                throw new PayPalApiException("Empty token response from PayPal");
            }
            token = resp.getBody().access_token;
            // trừ đi 60s để tránh dùng token sắp hết hạn
            tokenExpMs = now + (Math.max(resp.getBody().expires_in - 60, 60)) * 1000L;
            return token;
        } catch (RestClientResponseException ex) {
            throw new PayPalApiException("PayPal OAuth failed: " + ex.getRawStatusCode()
                    + " - " + ex.getResponseBodyAsString());
        }
    }

    private void throwExceptionForNonOKResponse(HttpStatus code) throws PayPalApiException {
        String msg;
        switch (code) {
            case BAD_REQUEST:            msg = "Bad Request to PayPal API"; break;
            case UNAUTHORIZED:           msg = "Unauthorized: invalid/expired token"; break;
            case FORBIDDEN:              msg = "Forbidden: insufficient permissions"; break;
            case NOT_FOUND:              msg = "Order ID not found"; break;
            case UNPROCESSABLE_ENTITY:   msg = "Unprocessable Entity: wrong state"; break;
            case INTERNAL_SERVER_ERROR:  msg = "PayPal server error"; break;
            case SERVICE_UNAVAILABLE:    msg = "PayPal service unavailable"; break;
            default:                     msg = "PayPal returned non-OK: " + code;
        }
        throw new PayPalApiException(msg);
    }

    static class TokenResponse {
        public String access_token;
        public int    expires_in;
        public String token_type;
        public String scope;
    }
}
