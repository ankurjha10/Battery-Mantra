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

    private static final String BASE_URL = "http://sms.tddigitalsolution.com/http-tokenkeyapi.php";
    private static final String AUTH_KEY = "323556494b4153323030383539301728374563";
    private static final String ROUTE = "1";
    private static final String SENDER_ID = "BATRYM";

    private static final String WA_BASE_URL = "http://wacontrol.ambeytech.com/api/wapi";
    private static final String WA_API_KEY = "fc8a1dbb72d014e62475c42ac478022f";

    @Value("${admin.phone:8057965238}")
    private String adminPhone;

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

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("authentic-key", AUTH_KEY)
                    .queryParam("senderid", SENDER_ID)
                    .queryParam("route", ROUTE)
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

    private void sendWhatsapp(String phone, String message) {
        try {
            if (phone == null || phone.isBlank()) {
                return;
            }
            String cleanPhone = phone.trim();
            if ("ADMIN".equalsIgnoreCase(cleanPhone)) {
                cleanPhone = adminPhone;
            }
            if (cleanPhone.length() == 10) {
                cleanPhone = "91" + cleanPhone;
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(WA_BASE_URL)
                    .queryParam("apikey", WA_API_KEY)
                    .queryParam("mobile", cleanPhone)
                    .queryParam("msg", message);

            java.net.URI uri = builder.build().encode().toUri();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            log.info("WhatsApp sent to {}. Response: {}", cleanPhone, response.getBody());
        } catch (Exception e) {
            log.error("Failed to send WhatsApp to {}. Error: {}", phone, e.getMessage());
        }
    }

    public void sendOtp(String phone, String name, String otp) {
        String templateId = "1707172906349288415";
        String customerName = (name != null && !name.isBlank()) ? name : "Customer";
        String message = String.format("Dear %s Your New OTP is %s For Your Battery Mantra Account https://www.batterymantra.com", customerName, otp);
        sendSms(phone, message, templateId);
        sendWhatsapp(phone, message);
    }

    public void sendRegistrationSms(String phone, String name) {
        String templateId = "1707172906056997301";
        String customerName = (name != null && !name.isBlank()) ? name : "Customer";
        String message = String.format("Dear %s, Thank You for Registation. FROM: Battery Mantra", customerName);
        sendSms(phone, message, templateId);
        sendWhatsapp(phone, message);
    }

    public void sendAdminOrderAlert(String phone, String orderId) {
        String templateId = "1707172906419435636";
        String message = String.format("Dear Admin,Congratulations! You got a New Order with Order Id :%s. FROM : Battery Mantra", orderId);
        sendSms(phone, message, templateId);
        sendWhatsapp(phone, message);
    }

    public void sendOrderPlacedSms(String phone, String name, String orderId) {
        String templateId = "1707172906339048747";
        String customerName = (name != null && !name.isBlank()) ? name : "Customer";
        String message = String.format("Dear %s, Your order has been placed successfully. Order Id : %s. FROM : Battery Mantra", customerName, orderId);
        sendSms(phone, message, templateId);
        sendWhatsapp(phone, message);
    }

    public void sendOrderDispatchedSms(String phone, String name, String orderId, String engineerName, String engineerPhone, String securityCode) {
        String templateId = "1707172906036030181";
        String customerName = (name != null && !name.isBlank()) ? name : "Customer";
        String engName = (engineerName != null && !engineerName.isBlank()) ? engineerName : "Engineer";
        String engPhone = (engineerPhone != null && !engineerPhone.isBlank()) ? engineerPhone : "N/A";
        String code = (securityCode != null && !securityCode.isBlank()) ? securityCode : "1234";
        String message = String.format("Dear %s your order has been dispatched for order id %s and arriving soon by our engineer ( %s +91 %s ). Your Delivery Security Code is %s FROM : Battery Mantra", 
                customerName, orderId, engName, engPhone, code);
        sendSms(phone, message, templateId);
        sendWhatsapp(phone, message);
    }

    public void sendOrderDeliveredSms(String phone, String name, String orderId) {
        String templateId = "1707172906101228231";
        String customerName = (name != null && !name.isBlank()) ? name : "Customer";
        String message = String.format("Dear %s, Your order has been delivered successfully for order id %s. FROM : Battery Mantra", customerName, orderId);
        sendSms(phone, message, templateId);
        sendWhatsapp(phone, message);
    }

    public void sendOrderCancelledSms(String phone, String name, String orderId) {
        String templateId = "1707172906092467307";
        String customerName = (name != null && !name.isBlank()) ? name : "Customer";
        String message = String.format("Dear %s, Your order has been cancelled. for order id %s. FROM : Battery Mantra", customerName, orderId);
        sendSms(phone, message, templateId);
        sendWhatsapp(phone, message);
    }
}
