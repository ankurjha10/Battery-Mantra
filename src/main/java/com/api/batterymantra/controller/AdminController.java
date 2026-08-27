package com.api.batterymantra.controller;

import com.api.batterymantra.dto.product.ProductListResponse;

import com.api.batterymantra.dto.admin.UserResponse;
import com.api.batterymantra.dto.admin.AdminCreateSubAdminRequest;
import com.api.batterymantra.dto.admin.AdminUpdateSubAdminRequest;
import com.api.batterymantra.dto.user.UserProfileResponse;
import com.api.batterymantra.service.AdminService;
import com.api.batterymantra.service.UserService;

import com.api.batterymantra.dto.category.CategoryListResponse;
import com.api.batterymantra.dto.category.CategoryReorderRequest;
import com.api.batterymantra.dto.category.CreateCategoryRequest;
import com.api.batterymantra.dto.category.UpdateCategoryRequest;
import com.api.batterymantra.service.CategoryService;

import com.api.batterymantra.dto.brand.BrandRequest;
import com.api.batterymantra.dto.brand.BrandResponse;
import com.api.batterymantra.service.BrandService;

import com.api.batterymantra.dto.vehicle.CreateVehicleRequest;
import com.api.batterymantra.dto.vehicle.VehicleResponse;
import com.api.batterymantra.service.VehicleService;

import com.api.batterymantra.dto.product.CreateProductRequest;
import com.api.batterymantra.dto.product.ProductDetailResponse;
import com.api.batterymantra.dto.product.ProductReorderRequest;
import com.api.batterymantra.dto.product.UpdateProductRequest;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.service.ProductService;

import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.dto.order.OrderStatusUpdateRequest;
import com.api.batterymantra.service.OrderService;

import com.api.batterymantra.dto.banner.BannerResponse;
import com.api.batterymantra.dto.banner.CreateBannerRequest;
import com.api.batterymantra.dto.banner.UpdateBannerRequest;
import com.api.batterymantra.service.BannerService;

import com.api.batterymantra.dto.callback.CallbackResponse;
import com.api.batterymantra.dto.callback.UpdateCallbackStatusRequest;
import com.api.batterymantra.service.CallbackRequestService;

import com.api.batterymantra.dto.enquiry.EnquiryResponse;
import com.api.batterymantra.dto.enquiry.UpdateEnquiryStatusRequest;
import com.api.batterymantra.entity.enums.EnquiryType;
import com.api.batterymantra.service.EnquiryService;

import com.api.batterymantra.dto.admin.AdminCreateCustomerRequest;
import com.api.batterymantra.service.EngineerAttendanceService;
import com.api.batterymantra.dto.engineer.UpdateLeaveStatusRequest;
import com.api.batterymantra.entity.EngineerAttendance;
import com.api.batterymantra.entity.LeaveRequest;
import com.api.batterymantra.entity.enums.LeaveStatus;
import com.api.batterymantra.dto.order.AdminCreateOrderRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final VehicleService vehicleService;
    private final ProductService productService;
    private final OrderService orderService;
    private final BannerService bannerService;
    private final CallbackRequestService callbackRequestService;
    private final EnquiryService enquiryService;
    private final EngineerAttendanceService attendanceService;
    private final UserService userService;

    // --- Users ---
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/sub-admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> createSubAdmin(@RequestBody @Valid AdminCreateSubAdminRequest request) {
        return new ResponseEntity<>(userService.createSubAdmin(request), HttpStatus.CREATED);
    }

    @PutMapping("/sub-admins/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> updateSubAdmin(
            @PathVariable UUID userId,
            @RequestBody @Valid AdminUpdateSubAdminRequest request) {
        return ResponseEntity.ok(userService.updateSubAdmin(userId, request));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleUserStatus(
            @PathVariable UUID userId,
            @RequestParam boolean isActive) {
        userService.toggleUserStatus(userId, isActive);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createCustomer(@RequestBody @Valid AdminCreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCustomer(request));
    }

    // --- Categories ---
    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryListResponse> addCategory(@RequestBody CreateCategoryRequest category) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.addCategory(category));
    }

    @DeleteMapping("/categories/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategoryById(@PathVariable UUID id) {
        categoryService.deleteCategoryById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/categories/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryListResponse> updateCategoryById(
            @PathVariable UUID id,
            @RequestBody UpdateCategoryRequest category) {
        return ResponseEntity.ok(categoryService.updateCategory(id, category));
    }

    @PutMapping("/categories/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorderCategories(@RequestBody List<CategoryReorderRequest> requests) {
        categoryService.reorderCategories(requests);
        return ResponseEntity.ok().build();
    }

    // --- Brands ---
    @PostMapping("/brands")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> createBrand(@RequestBody @Valid BrandRequest brandRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.createBrand(brandRequest));
    }

    @PutMapping("/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> updateBrand(@PathVariable UUID id,
            @RequestBody @Valid BrandRequest brandRequest) {
        return ResponseEntity.ok(brandService.updateBrand(id, brandRequest));
    }

    @DeleteMapping("/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBrand(@PathVariable UUID id) {
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }

    // --- Vehicles ---
    @PostMapping("/vehicles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleResponse> createVehicle(@RequestBody CreateVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.createVehicle(request));
    }

    @PutMapping("/vehicles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleResponse> updateVehicle(@PathVariable UUID id,
            @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request));
    }

    @DeleteMapping("/vehicles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    // --- Products ---
    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> addProduct(
            @RequestBody CreateProductRequest product,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(product));
    }

    @PatchMapping("/products/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> updateProduct(
            @PathVariable UUID id,
            @RequestBody UpdateProductRequest product,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/products/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        productService.deleteProductById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products/pending-approvals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductListResponse>> getPendingApprovalProducts() {
        return ResponseEntity.ok(productService.getPendingApprovalProducts());
    }

    @PatchMapping("/products/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> approveProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.approveProduct(id));
    }

    @PutMapping("/products/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorderProducts(@RequestBody List<ProductReorderRequest> requests) {
        productService.reorderProducts(requests);
        return ResponseEntity.ok().build();
    }

    // --- Orders ---
    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid AdminCreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createAdminOrder(request));
    }

    @PatchMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody @Valid OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request.getOrderStatus()));
    }

    @PatchMapping("/orders/{orderId}/assign-partner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> assignOrderToPartner(
            @PathVariable UUID orderId,
            @RequestParam UUID partnerId) {
        return ResponseEntity.ok(orderService.assignPartner(orderId, partnerId));
    }

    @PatchMapping("/orders/{orderId}/assign-engineer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> assignOrderToEngineer(
            @PathVariable UUID orderId,
            @RequestParam UUID engineerId) {
        return ResponseEntity.ok(orderService.assignEngineerByAdmin(orderId, engineerId));
    }

    // --- Banners ---
    @GetMapping("/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BannerResponse>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @PostMapping("/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponse> createBanner(@RequestBody @Valid CreateBannerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.createBanner(request));
    }

    @PutMapping("/banners/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponse> updateBanner(@PathVariable UUID id,
            @RequestBody @Valid UpdateBannerRequest request) {
        return ResponseEntity.ok(bannerService.updateBanner(id, request));
    }

    @DeleteMapping("/banners/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBanner(@PathVariable UUID id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }

    // --- Callbacks ---
    @GetMapping("/callbacks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CallbackResponse>> getAllCallbacks() {
        return ResponseEntity.ok(callbackRequestService.getAllCallbackRequests());
    }

    @PatchMapping("/callbacks/{callbackId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CallbackResponse> updateCallbackStatus(@PathVariable Long callbackId,
            @RequestBody UpdateCallbackStatusRequest request) {
        return ResponseEntity.ok(callbackRequestService.updateCallbackStatus(callbackId, request.getStatus()));
    }

    // --- Enquiries ---
    @GetMapping("/enquiries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EnquiryResponse>> getAllEnquiries(
            @RequestParam(required = false) EnquiryType type) {
        return ResponseEntity.ok(enquiryService.getAllEnquiries(type));
    }

    @PatchMapping("/enquiries/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnquiryResponse> updateEnquiryStatus(@PathVariable Long id,
            @RequestBody UpdateEnquiryStatusRequest request) {
        return ResponseEntity.ok(enquiryService.updateEnquiryStatus(id, request.getStatus()));
    }

    // --- Engineer Attendance & Leaves ---
    @GetMapping("/engineers/{engineerId}/attendance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EngineerAttendance>> getEngineerAttendance(@PathVariable UUID engineerId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByEngineerId(engineerId));
    }

    @PatchMapping("/leave-requests/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveRequest> updateLeaveStatus(@PathVariable UUID id,
            @RequestBody UpdateLeaveStatusRequest request) {
        LeaveStatus status = LeaveStatus.valueOf(request.getStatus().toUpperCase());
        return ResponseEntity.ok(attendanceService.updateLeaveStatus(id, status));
    }

    @GetMapping("/leave-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaveRequest>> getAllLeaveRequests() {
        return ResponseEntity.ok(attendanceService.getAllLeaveRequests());
    }

}
