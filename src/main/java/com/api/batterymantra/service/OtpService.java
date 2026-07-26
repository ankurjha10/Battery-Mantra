package com.api.batterymantra.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static class OtpData {
        String otp;
        long expiryTime;

        OtpData(String otp, long expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }

    // Stores phone number -> OtpData
    private final Map<String, OtpData> otpCache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // 5 minutes in milliseconds
    private static final long OTP_VALIDITY_DURATION_MS = 5 * 60 * 1000;

    public String generateOtp(String phoneNumber) {
        // Generate a 4-digit OTP (1000 to 9999)
        String otp = String.valueOf(1000 + random.nextInt(9000));
        long expiryTime = System.currentTimeMillis() + OTP_VALIDITY_DURATION_MS;
        
        otpCache.put(phoneNumber, new OtpData(otp, expiryTime));
        return otp;
    }

    public boolean verifyOtp(String phoneNumber, String otp) {
        OtpData otpData = otpCache.get(phoneNumber);
        if (otpData == null) {
            return false;
        }

        if (System.currentTimeMillis() > otpData.expiryTime) {
            otpCache.remove(phoneNumber); // Expired
            return false;
        }

        if (otpData.otp.equals(otp)) {
            otpCache.remove(phoneNumber); // Successfully verified, remove to prevent reuse
            return true;
        }

        return false;
    }
}
