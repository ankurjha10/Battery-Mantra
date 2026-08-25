package com.api.batterymantra.service;

import com.api.batterymantra.entity.Orders;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.repository.OrderRepository;
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
            count++;
        }

        log.info("Successfully auto-cancelled {} PENDING orders older than 10 days.", count);
    }
}
