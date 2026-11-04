package com.api.batterymantra.controller;

import com.api.batterymantra.controller.admin.CentralInventoryController;
import com.api.batterymantra.dto.inventory.*;
import com.api.batterymantra.entity.enums.StockStatus;
import com.api.batterymantra.service.CentralInventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CentralInventoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CentralInventoryService inventoryService;

    @InjectMocks
    private CentralInventoryController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void shouldGetInventoryDashboard() throws Exception {
        InventorySummaryDto summary = InventorySummaryDto.builder()
                .totalSkus(100)
                .inStockCount(80)
                .lowStockCount(15)
                .outOfStockCount(5)
                .lowStockThreshold(10)
                .build();

        UUID prodId = UUID.randomUUID();
        InventoryItemDto item = InventoryItemDto.builder()
                .productId(prodId)
                .productName("Exide Mileage")
                .productStock(4)
                .lowStockThreshold(10)
                .stockStatus(StockStatus.LOW_STOCK)
                .productPrice(BigDecimal.valueOf(4200))
                .build();

        InventoryDashboardResponse response = InventoryDashboardResponse.builder()
                .summary(summary)
                .items(new PageImpl<>(List.of(item), org.springframework.data.domain.PageRequest.of(0, 20), 1))
                .build();

        when(inventoryService.getInventoryDashboard(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/admin/inventory")
                        .param("status", "LOW_STOCK")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalSkus").value(100))
                .andExpect(jsonPath("$.summary.lowStockCount").value(15))
                .andExpect(jsonPath("$.items.content[0].productName").value("Exide Mileage"))
                .andExpect(jsonPath("$.items.content[0].stockStatus").value("LOW_STOCK"));
    }

    @Test
    void shouldUpdateProductStockInline() throws Exception {
        UUID prodId = UUID.randomUUID();
        UpdateStockRequest req = new UpdateStockRequest(25, "Restocked");
        UpdateStockResponse resp = UpdateStockResponse.builder()
                .productId(prodId)
                .productStock(25)
                .stockStatus(StockStatus.IN_STOCK)
                .message("Stock updated successfully")
                .build();

        when(inventoryService.updateProductStock(eq(prodId), any(), any()))
                .thenReturn(resp);

        mockMvc.perform(patch("/api/admin/inventory/{productId}/stock", prodId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(prodId.toString()))
                .andExpect(jsonPath("$.productStock").value(25))
                .andExpect(jsonPath("$.stockStatus").value("IN_STOCK"))
                .andExpect(jsonPath("$.message").value("Stock updated successfully"));
    }

    @Test
    void shouldBulkUpdateStock() throws Exception {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        List<BulkStockUpdateRequest> requests = List.of(
                new BulkStockUpdateRequest(p1, 15, "Batch 1"),
                new BulkStockUpdateRequest(p2, 0, "Batch 2")
        );

        when(inventoryService.bulkUpdateStock(any(), any()))
                .thenReturn(new BulkStockUpdateResponse(2, "Bulk stock updated successfully"));

        mockMvc.perform(put("/api/admin/inventory/bulk-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(2))
                .andExpect(jsonPath("$.message").value("Bulk stock updated successfully"));
    }
}

