package com.api.batterymantra.service;

import com.api.batterymantra.dto.blog.*;
import com.api.batterymantra.dto.product.ProductListResponse;
import com.api.batterymantra.entity.BlogPost;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.enums.BlogStatus;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.BlogPostRepository;
import com.api.batterymantra.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogPostRepository blogPostRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private BlogViewCounterService blogViewCounterService;

    @InjectMocks
    private BlogService blogService;

    private UUID sampleId;
    private BlogPost samplePost;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        samplePost = BlogPost.builder()
                .id(sampleId)
                .title("Complete Guide to Car Battery Care")
                .slug("complete-guide-to-car-battery-care")
                .excerpt("Learn how to maintain your car battery.")
                .content("<p>Proper car battery care ensures long vehicle life and reliable starts every morning.</p>")
                .category("Maintenance")
                .tags("car,battery,maintenance")
                .authorName("Battery Mantra Team")
                .readTimeMinutes(3)
                .status(BlogStatus.PUBLISHED)
                .isFeatured(true)
                .viewCount(100L)
                .publishedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .relatedProductIds(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("Public Blog Queries")
    class PublicBlogQueries {

        @Test
        @DisplayName("getPublicBlogs should return paginated list of published posts")
        void getPublicBlogs_success() {
            Page<BlogPost> page = new PageImpl<>(List.of(samplePost));
            when(blogPostRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            Page<BlogPostListResponse> result = blogService.getPublicBlogs(0, 10, "Maintenance", "car", "maintenance", true);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            BlogPostListResponse first = result.getContent().get(0);
            assertEquals("Complete Guide to Car Battery Care", first.getTitle());
            assertEquals("complete-guide-to-car-battery-care", first.getSlug());
            assertEquals("PUBLISHED", first.getStatus());
        }

        @Test
        @DisplayName("getPublicBlogBySlug should fetch published post, increment view count, and hydrate related products")
        void getPublicBlogBySlug_success() {
            UUID prodId = UUID.randomUUID();
            samplePost.setRelatedProductIds(List.of(prodId));

            Product product = new Product();
            product.setProductId(prodId);
            product.setProductName("Amaron Current Pro 45Ah");

            ProductListResponse productResp = new ProductListResponse();
            productResp.setProductId(prodId);
            productResp.setProductName("Amaron Current Pro 45Ah");

            when(blogPostRepository.findBySlugAndStatus("complete-guide-to-car-battery-care", BlogStatus.PUBLISHED))
                    .thenReturn(Optional.of(samplePost));
            when(productRepository.findAllById(List.of(prodId))).thenReturn(List.of(product));
            when(productService.toListResponse(product, null)).thenReturn(productResp);

            BlogPostDetailResponse response = blogService.getPublicBlogBySlug("complete-guide-to-car-battery-care");

            assertNotNull(response);
            assertEquals(sampleId, response.getId());
            assertEquals("complete-guide-to-car-battery-care", response.getSlug());
            assertEquals(1, response.getRelatedProducts().size());
            assertEquals("Amaron Current Pro 45Ah", response.getRelatedProducts().get(0).getProductName());

            verify(blogViewCounterService, times(1)).incrementViewCount(sampleId);
        }

        @Test
        @DisplayName("getPublicBlogBySlug should throw 404 when not found or not published")
        void getPublicBlogBySlug_notFound() {
            when(blogPostRepository.findBySlugAndStatus("draft-slug", BlogStatus.PUBLISHED))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> blogService.getPublicBlogBySlug("draft-slug"));
            verify(blogViewCounterService, never()).incrementViewCount(any());
        }

        @Test
        @DisplayName("getCategoriesWithCounts should return distinct categories with counts")
        void getCategoriesWithCounts_success() {
            List<BlogCategoryCountResponse> counts = List.of(
                    new BlogCategoryCountResponse("Car Batteries", 8L),
                    new BlogCategoryCountResponse("Maintenance", 4L)
            );
            when(blogPostRepository.countPostsByCategory(BlogStatus.PUBLISHED)).thenReturn(counts);

            List<BlogCategoryCountResponse> result = blogService.getCategoriesWithCounts();

            assertEquals(2, result.size());
            assertEquals("Car Batteries", result.get(0).getCategory());
            assertEquals(8L, result.get(0).getPostCount());
        }

        @Test
        @DisplayName("getFeaturedBlogs should return top 3 featured blogs")
        void getFeaturedBlogs_success() {
            when(blogPostRepository.findTop3ByIsFeaturedTrueAndStatusOrderByPublishedAtDescCreatedAtDesc(BlogStatus.PUBLISHED))
                    .thenReturn(List.of(samplePost));

            List<BlogPostListResponse> result = blogService.getFeaturedBlogs();

            assertEquals(1, result.size());
            assertTrue(result.get(0).getIsFeatured());
        }
    }

    @Nested
    @DisplayName("Admin Blog CRUD & Business Rules")
    class AdminBlogCrud {

        @Test
        @DisplayName("createBlog should auto-slugify title when slug is omitted and handle collision")
        void createBlog_autoSlugifyAndCollision() {
            CreateBlogPostRequest request = CreateBlogPostRequest.builder()
                    .title("Best Inverter Batteries 2026!")
                    .content("<p>Review of top inverters</p>")
                    .category("Inverter & Solar")
                    .status("PUBLISHED")
                    .build();

            when(blogPostRepository.existsBySlug("best-inverter-batteries-2026")).thenReturn(true);
            when(blogPostRepository.existsBySlug("best-inverter-batteries-2026-1")).thenReturn(false);

            when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(invocation -> {
                BlogPost saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                saved.setCreatedAt(Instant.now());
                return saved;
            });

            BlogPostDetailResponse response = blogService.createBlog(request);

            assertNotNull(response);
            assertEquals("best-inverter-batteries-2026-1", response.getSlug());
            assertEquals("PUBLISHED", response.getStatus());
            assertNotNull(response.getPublishedAt());
            assertEquals("Battery Mantra Team", response.getAuthorName());
        }

        @Test
        @DisplayName("createBlog should calculate read time if omitted")
        void createBlog_calculateReadTime() {
            // 400 words = 2 minutes (400 / 200 = 2)
            StringBuilder sb = new StringBuilder("<p>");
            for (int i = 0; i < 400; i++) {
                sb.append("word").append(i).append(" ");
            }
            sb.append("</p>");

            CreateBlogPostRequest request = CreateBlogPostRequest.builder()
                    .title("Wordy Post")
                    .content(sb.toString())
                    .category("Guides")
                    .readTimeMinutes(0)
                    .build();

            when(blogPostRepository.existsBySlug("wordy-post")).thenReturn(false);
            when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(invocation -> {
                BlogPost saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            BlogPostDetailResponse response = blogService.createBlog(request);

            assertEquals(2, response.getReadTimeMinutes());
            assertEquals("DRAFT", response.getStatus());
            assertNull(response.getPublishedAt());
        }

        @Test
        @DisplayName("updateBlog should update fields and set publishedAt when status transitions to PUBLISHED")
        void updateBlog_statusTransition() {
            BlogPost existing = BlogPost.builder()
                    .id(sampleId)
                    .title("Old Title")
                    .slug("old-title")
                    .content("<p>Old Content</p>")
                    .category("Guides")
                    .status(BlogStatus.DRAFT)
                    .publishedAt(null)
                    .build();

            UpdateBlogPostRequest updateRequest = new UpdateBlogPostRequest();
            updateRequest.setTitle("Updated Title");
            updateRequest.setContent("<p>Updated Content</p>");
            updateRequest.setCategory("Guides");
            updateRequest.setStatus("PUBLISHED");

            when(blogPostRepository.findById(sampleId)).thenReturn(Optional.of(existing));
            when(blogPostRepository.existsBySlugAndIdNot("updated-title", sampleId)).thenReturn(false);
            when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

            BlogPostDetailResponse response = blogService.updateBlog(sampleId, updateRequest);

            assertEquals("Updated Title", response.getTitle());
            assertEquals("updated-title", response.getSlug());
            assertEquals("PUBLISHED", response.getStatus());
            assertNotNull(response.getPublishedAt());
        }

        @Test
        @DisplayName("updateBlogStatus should toggle status and set publishedAt")
        void updateBlogStatus_toggle() {
            BlogPost existing = BlogPost.builder()
                    .id(sampleId)
                    .title("Draft Title")
                    .status(BlogStatus.DRAFT)
                    .publishedAt(null)
                    .build();

            UpdateBlogStatusRequest statusRequest = new UpdateBlogStatusRequest("PUBLISHED");

            when(blogPostRepository.findById(sampleId)).thenReturn(Optional.of(existing));
            when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

            BlogPostDetailResponse response = blogService.updateBlogStatus(sampleId, statusRequest);

            assertEquals("PUBLISHED", response.getStatus());
            assertNotNull(response.getPublishedAt());
        }

        @Test
        @DisplayName("deleteBlog should delete existing post or throw 404")
        void deleteBlog_success() {
            when(blogPostRepository.findById(sampleId)).thenReturn(Optional.of(samplePost));

            blogService.deleteBlog(sampleId);

            verify(blogPostRepository, times(1)).delete(samplePost);
        }

        @Test
        @DisplayName("deleteBlog should throw ResourceNotFoundException when id does not exist")
        void deleteBlog_notFound() {
            UUID unknown = UUID.randomUUID();
            when(blogPostRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> blogService.deleteBlog(unknown));
            verify(blogPostRepository, never()).delete(any(BlogPost.class));
        }
    }
}

