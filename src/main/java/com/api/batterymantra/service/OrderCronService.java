package com.api.batterymantra.service;

import com.api.batterymantra.entity.Orders;
import com.api.batterymantra.entity.PendingPaymentCheckout;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.entity.enums.PaymentCheckoutStatus;
import com.api.batterymantra.repository.OrderRepository;
import com.api.batterymantra.repository.PendingPaymentCheckoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCronService {

    private final OrderRepository orderRepository;
    private final PendingPaymentCheckoutRepository pendingPaymentCheckoutRepository;
    private final CentralInventoryService centralInventoryService;

    /**
     * Runs every 15 minutes.
     * Marks abandoned/timed-out pending online payment checkouts as EXPIRED.
     * No order was ever created and no stock was deducted for these checkouts.
     */
    @Scheduled(cron = "0 */15 * * * ?")
    @Transactional
    public void expireAbandonedPendingCheckouts() {
        List<PendingPaymentCheckout> expiredCheckouts = pendingPaymentCheckoutRepository
                .findByStatusAndExpiresAtBefore(PaymentCheckoutStatus.PENDING, LocalDateTime.now());

        if (expiredCheckouts.isEmpty()) {
            return;
        }

        for (PendingPaymentCheckout checkout : expiredCheckouts) {
            checkout.setStatus(PaymentCheckoutStatus.EXPIRED);
            checkout.setFailureReason("Customer - Payment timeout (checkout expired)");
        }
        pendingPaymentCheckoutRepository.saveAll(expiredCheckouts);

        log.info("Expired {} abandoned pending online payment checkouts.", expiredCheckouts.size());
    }

    /**
     * Runs every day at midnight (00:00:00).
     * Scans for PENDING orders created more than 10 days ago and cancels them.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void autoCancelOldPendingOrders() {
        log.info("Starting cron job: autoCancelOldPendingOrders");

        LocalDateTime tenDaysAgo = LocalDateTime.now().minusDays(10);

        List<Orders> oldPendingOrders = orderRepository.findByOrderStatusAndPlacedAtBefore(
                OrderStatus.PENDING, tenDaysAgo);

        if (oldPendingOrders.isEmpty()) {
            log.info("No PENDING orders found older than 10 days.");
            return;
        }

        int count = 0;
        for (Orders order : oldPendingOrders) {
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setCancellationReason("Auto-cancelled due to 10-day timeout");
            orderRepository.save(order);
            centralInventoryService.restoreStockForOrder(order, "SYSTEM", order.getCancellationReason());
            count++;
        }

        log.info("Successfully auto-cancelled {} PENDING orders older than 10 days.", count);
    }
}
