package com.api.batterymantra.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class SmsService {

    private final RestTemplate restTemplate;

    @Value("${sms.base.url}")
    private String smsBaseUrl;

    @Value("${sms.auth.key}")
    private String smsAuthKey;

    @Value("${sms.route}")
    private String smsRoute;

    @Value("${sms.sender.id}")
    private String smsSenderId;

    @Value("${sms.wa.base.url}")
    private String smsWaBaseUrl;

    @Value("${sms.wa.api.key}")
    private String smsWaApiKey;

    @Value("${sms.wa.sender.number:+918282825280}")
    private String smsWaSenderNumber;

    @Value("${admin.phone:8282825280}")
    private String adminPhone;

    @Value("${app.frontend.url:https://batterymantra.com}")
    private String frontendUrl;

    public SmsService() {
        this.restTemplate = new RestTemplate();
    }

    private void sendSms(String phone, String message, String templateId) {
        try {
            if (phone == null || phone.isBlank()) {
                return;
            }
            String cleanPhone = phone.trim();
            if ("ADMIN".equalsIgnoreCase(cleanPhone)) {
                cleanPhone = adminPhone;
            }
            // Ensure phone has 91 prefix for Indian numbers if it's exactly 10 digits
            if (cleanPhone.length() == 10) {
                cleanPhone = "91" + cleanPhone;
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(smsBaseUrl)
                    .queryParam("authentic-key", smsAuthKey)
                    .queryParam("senderid", smsSenderId)
                    .queryParam("route", smsRoute)
                    .queryParam("number", cleanPhone)
                    .queryParam("message", message)
                    .queryParam("templateid", templateId);

            java.net.URI uri = builder.build().encode().toUri();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            log.info("SMS sent to {}. Template: {}. Response: {}", cleanPhone, templateId, response.getBody());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}. Template: {}. Error: {}", phone, templateId, e.getMessage());
        }
    }

    private void sendWhatsapp(String phone, String templateName, String... params) {
        try {
            if (phone == null || phone.isBlank()) return;
            String cleanPhone = phone.trim();
            if ("ADMIN".equalsIgnoreCase(cleanPhone)) {
                cleanPhone = adminPhone;
            }
            if (cleanPhone.length() == 10) {
                cleanPhone = "+91" + cleanPhone;
            } else if (!cleanPhone.startsWith("+")) {
                cleanPhone = "+" + cleanPhone;
            }
            
            String paramsJson = java.util.Arrays.stream(params)
                .map(p -> "\"" + (p == null ? "" : p.replace("\"", "\\\"").replace("\n", "\\n")) + "\"")
                .collect(java.util.stream.Collectors.joining(","));
                
            String jsonPayload = String.format(
                "{\"from\": \"%s\", \"campaignName\": \"api-test\", \"to\": \"%s\", \"templateName\": \"%s\", \"components\": {\"body\":{\"params\":[%s]}}, \"type\": \"template\"}",
                smsWaSenderNumber, cleanPhone, templateName, paramsJson
            );

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("apiKey", smsWaApiKey);

            org.springframework.http.HttpEntity<String> request = new org.springframework.http.HttpEntity<>(jsonPayload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(smsWaBaseUrl, request, String.class);
            log.info("WhatsApp sent to {} with template {}. Response: {}", cleanPhone, templateName, response.getBody());
        } catch (Exception e) {
            log.error("Failed to send WhatsApp to {}. Template: {}. Error: {}", phone, templateName, e.getMessage());
        }
    }

    public void sendOtp(String phone, String name, String otp) {
        String templateId = "1707172906349288415";
        String customerName = (name != null && !name.isBlank()) ? name : "Customer";
        
        // SMS Message
        String smsMessage = String.format("Dear %s Your New OTP is %s For Your Battery Mantra Account %s", customerName, otp, frontendUrl);
        sendSms(phone, smsMessage, templateId);
        
        // WhatsApp Message
        sendWhatsapp(phone, "otp_for_sign_up", otp);
    }

    public void sendRegistrationSms(String phone, String name) {
        String templateId = "1707172911246995646";
        String customerName = (name != null && !name.isBlank()) ? name : "Customer";
        
        // SMS Message
        String smsMessage = String.format("Dear %s , Thank you for choosing Battery Mantra. You have registered successfully your Battery Mantra account. Get lowest price for Car, Inverter Battery and Many More.. %s", customerName, frontendUrl);
        sendSms(phone, smsMessage, templateId);
        
        // No AOC Whatsapp Template found for registration, skip WhatsApp here or use a default if it existed.
    }

    public void sendOrderPlacedSms(String phone, String customerName, String orderId, String amount, String date, String productName, String paymentMode) {
        String templateId = "1707172911369348906";
        
        // SMS Message
        String smsMessage = String.format("Dear %s , Your order %s has been placed successfully for order id %s . Thank you for ordering with us. You can track your order from %s", customerName, productName, orderId, frontendUrl);
        sendSms(phone, smsMessage, templateId);
        
        // WhatsApp Message to Customer
        sendWhatsapp(phone, "new_order_customer", customerName, orderId, date, amount, paymentMode, productName);
        
        // WhatsApp Message to Admin
        sendWhatsapp("ADMIN", "new_order_admins", customerName, orderId, date, amount, paymentMode);
    }

    public void sendOrderDispatchedSms(String phone, String customerName, String productName, String orderId, String engineerName, String engineerPhone, String securityCode) {
        String templateId = "1707172911579717144";
        
        // SMS Message
        String smsMessage = String.format("Dear %s , Your order has been dispatched (%s) for order id %s and arriving soon by (Name: %s, Mobile No.: %s). Your Delivery Security Code is %s. Thank you %s", customerName, productName, orderId, engineerName, engineerPhone, securityCode, frontendUrl);
        sendSms(phone, smsMessage, templateId);
        
        // WhatsApp Message
        sendWhatsapp(phone, "order_dispatch", customerName, engineerName, engineerPhone);
    }

    public void sendOrderDeliveredSms(String phone, String customerName, String productName, String orderId) {
        String templateId = "1707172911533423405";
        
        // SMS Message
        String smsMessage = String.format("Dear %s , Your Battery Mantra order for %s has been delivered successfully for order id %s . Thank you %s", customerName, productName, orderId, frontendUrl);
        sendSms(phone, smsMessage, templateId);
        
        // WhatsApp Message
        sendWhatsapp(phone, "order_id", productName, frontendUrl);
    }

    public void sendOrderCancelledSms(String phone, String customerName, String productName, String orderId, String cancelReason) {
        String templateId = "1707172906092467307"; // fallback to order delivered SMS template? Or maybe they don't have SMS template for cancel.
        
        // WhatsApp Message
        sendWhatsapp(phone, "order_cancelled", customerName, orderId, cancelReason);
    }
}
