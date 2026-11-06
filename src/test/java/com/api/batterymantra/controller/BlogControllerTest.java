package com.api.batterymantra.controller;

import com.api.batterymantra.dto.blog.*;
import com.api.batterymantra.exception.GlobalExceptionHandler;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.service.BlogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BlogControllerTest {

    private MockMvc publicMockMvc;
    private MockMvc adminMockMvc;

    @Mock
    private BlogService blogService;

    @InjectMocks
    private BlogController blogController;

    @InjectMocks
    private BlogAdminController blogAdminController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        publicMockMvc = MockMvcBuilders.standaloneSetup(blogController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminMockMvc = MockMvcBuilders.standaloneSetup(blogAdminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("Public Blog Endpoints (/api/blogs)")
    class PublicBlogEndpoints {

        @Test
        @DisplayName("GET /api/blogs should return 200 with page of posts")
        void getPublishedBlogs_success() throws Exception {
            BlogPostListResponse item = BlogPostListResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Battery Maintenance Tips")
                    .slug("battery-maintenance-tips")
                    .status("PUBLISHED")
                    .build();

            when(blogService.getPublicBlogs(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(item)));

            publicMockMvc.perform(get("/api/blogs")
                            .param("page", "0")
                            .param("size", "10")
                            .param("category", "Maintenance")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Battery Maintenance Tips"))
                    .andExpect(jsonPath("$.content[0].slug").value("battery-maintenance-tips"));
        }

        @Test
        @DisplayName("GET /api/blogs/{slug} should return 200 with detail")
        void getBlogBySlug_success() throws Exception {
            BlogPostDetailResponse detail = BlogPostDetailResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Complete Inverter Guide")
                    .slug("complete-inverter-guide")
                    .content("<p>Inverter batteries are essential...</p>")
                    .status("PUBLISHED")
                    .build();

            when(blogService.getPublicBlogBySlug("complete-inverter-guide")).thenReturn(detail);

            publicMockMvc.perform(get("/api/blogs/complete-inverter-guide")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value("complete-inverter-guide"))
                    .andExpect(jsonPath("$.content").value("<p>Inverter batteries are essential...</p>"));
        }

        @Test
        @DisplayName("GET /api/blogs/{slug} should return 404 when post is not found")
        void getBlogBySlug_notFound() throws Exception {
            when(blogService.getPublicBlogBySlug("unknown-slug"))
                    .thenThrow(new ResourceNotFoundException("Blog post not found with slug: unknown-slug"));

            publicMockMvc.perform(get("/api/blogs/unknown-slug")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Blog post not found with slug: unknown-slug"));
        }

        @Test
        @DisplayName("GET /api/blogs/categories should return 200 with category counts")
        void getCategories_success() throws Exception {
            List<BlogCategoryCountResponse> counts = List.of(
                    new BlogCategoryCountResponse("Car Batteries", 5L),
                    new BlogCategoryCountResponse("Solar", 3L)
            );
            when(blogService.getCategoriesWithCounts()).thenReturn(counts);

            publicMockMvc.perform(get("/api/blogs/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].category").value("Car Batteries"))
                    .andExpect(jsonPath("$[0].postCount").value(5));
        }

        @Test
        @DisplayName("GET /api/blogs/featured should return 200 with featured list")
        void getFeaturedBlogs_success() throws Exception {
            BlogPostListResponse featured = BlogPostListResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Top 5 Inverters")
                    .isFeatured(true)
                    .build();

            when(blogService.getFeaturedBlogs()).thenReturn(List.of(featured));

            publicMockMvc.perform(get("/api/blogs/featured")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Top 5 Inverters"))
                    .andExpect(jsonPath("$[0].isFeatured").value(true));
        }
    }

    @Nested
    @DisplayName("Admin Blog Endpoints (/api/admin/blogs)")
    class AdminBlogEndpoints {

        @Test
        @DisplayName("GET /api/admin/blogs should return 200 with all blogs")
        void getAllBlogs_success() throws Exception {
            BlogPostListResponse item = BlogPostListResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Admin Draft Post")
                    .status("DRAFT")
                    .build();

            when(blogService.getAdminBlogs(anyInt(), anyInt(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(item)));

            adminMockMvc.perform(get("/api/admin/blogs")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Admin Draft Post"))
                    .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
        }

        @Test
        @DisplayName("GET /api/admin/blogs/{id} should return 200 with detail")
        void getBlogById_success() throws Exception {
            UUID id = UUID.randomUUID();
            BlogPostDetailResponse detail = BlogPostDetailResponse.builder()
                    .id(id)
                    .title("Admin Detail Post")
                    .status("DRAFT")
                    .build();

            when(blogService.getAdminBlogById(id)).thenReturn(detail);

            adminMockMvc.perform(get("/api/admin/blogs/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.title").value("Admin Detail Post"));
        }

        @Test
        @DisplayName("POST /api/admin/blogs should return 201 on valid request")
        void createBlog_success() throws Exception {
            CreateBlogPostRequest request = CreateBlogPostRequest.builder()
                    .title("New Car Battery Article")
                    .content("<p>Everything about batteries</p>")
                    .category("Car Batteries")
                    .status("DRAFT")
                    .build();

            BlogPostDetailResponse created = BlogPostDetailResponse.builder()
                    .id(UUID.randomUUID())
                    .title("New Car Battery Article")
                    .slug("new-car-battery-article")
                    .content("<p>Everything about batteries</p>")
                    .category("Car Batteries")
                    .status("DRAFT")
                    .readTimeMinutes(1)
                    .createdAt(Instant.now())
                    .build();

            when(blogService.createBlog(any(CreateBlogPostRequest.class))).thenReturn(created);

            adminMockMvc.perform(post("/api/admin/blogs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("New Car Battery Article"))
                    .andExpect(jsonPath("$.slug").value("new-car-battery-article"));
        }

        @Test
        @DisplayName("POST /api/admin/blogs should return 400 when required fields are missing")
        void createBlog_validationFailure() throws Exception {
            CreateBlogPostRequest request = CreateBlogPostRequest.builder()
                    .title("") // Blank title
                    .content("") // Blank content
                    .category("") // Blank category
                    .build();

            adminMockMvc.perform(post("/api/admin/blogs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT /api/admin/blogs/{id} should return 200 on update")
        void updateBlog_success() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateBlogPostRequest request = new UpdateBlogPostRequest();
            request.setTitle("Updated Article");
            request.setContent("<p>Updated body</p>");
            request.setCategory("Solar");
            request.setStatus("PUBLISHED");

            BlogPostDetailResponse updated = BlogPostDetailResponse.builder()
                    .id(id)
                    .title("Updated Article")
                    .slug("updated-article")
                    .content("<p>Updated body</p>")
                    .category("Solar")
                    .status("PUBLISHED")
                    .build();

            when(blogService.updateBlog(eq(id), any(UpdateBlogPostRequest.class))).thenReturn(updated);

            adminMockMvc.perform(put("/api/admin/blogs/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Article"))
                    .andExpect(jsonPath("$.status").value("PUBLISHED"));
        }

        @Test
        @DisplayName("PATCH /api/admin/blogs/{id}/status should return 200 on status toggle")
        void updateBlogStatus_success() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateBlogStatusRequest request = new UpdateBlogStatusRequest("PUBLISHED");

            BlogPostDetailResponse updated = BlogPostDetailResponse.builder()
                    .id(id)
                    .title("Toggled Article")
                    .status("PUBLISHED")
                    .publishedAt(Instant.now())
                    .build();

            when(blogService.updateBlogStatus(eq(id), any(UpdateBlogStatusRequest.class))).thenReturn(updated);

            adminMockMvc.perform(patch("/api/admin/blogs/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PUBLISHED"));
        }

        @Test
        @DisplayName("DELETE /api/admin/blogs/{id} should return 204")
        void deleteBlog_success() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(blogService).deleteBlog(id);

            adminMockMvc.perform(delete("/api/admin/blogs/{id}", id))
                    .andExpect(status().isNoContent());

            verify(blogService, times(1)).deleteBlog(id);
        }
    }
}

