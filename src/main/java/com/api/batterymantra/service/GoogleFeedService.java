package com.api.batterymantra.service;

import com.api.batterymantra.entity.Product;
import com.api.batterymantra.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleFeedService {

    private final ProductRepository productRepository;

    @Value("${app.frontend.url:https://batterymantra.com}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public String generateGoogleProductsFeed() {
        List<Product> products = productRepository.findAll();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss xmlns:g=\"http://base.google.com/ns/1.0\" version=\"2.0\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>Battery Mantra Products Feed</title>\n");
        xml.append("    <link>").append(frontendUrl).append("</link>\n");
        xml.append("    <description>Product feed for Battery Mantra</description>\n");

        for (Product product : products) {
            String title = product.getSeo() != null && product.getSeo().getMetaTitle() != null 
                    ? product.getSeo().getMetaTitle() 
                    : product.getProductName();
            String description = product.getSeo() != null && product.getSeo().getMetaDescription() != null 
                    ? product.getSeo().getMetaDescription() 
                    : (product.getProductDescription() != null ? product.getProductDescription() : title);
            
            String slug = product.getSeo() != null && product.getSeo().getSlug() != null
                    ? product.getSeo().getSlug()
                    : product.getProductId().toString();

            String link = frontendUrl + "/product/" + slug;
            
            // Assuming image URL is already absolute or relative to some base
            String imageLink = product.getProductImage();
            if (imageLink != null && !imageLink.startsWith("http")) {
                imageLink = "https://res.cloudinary.com/dwy48p5w1/image/upload/v1/" + imageLink; // Adjust as needed
            }

            xml.append("    <item>\n");
            xml.append("      <g:id>").append(product.getProductId()).append("</g:id>\n");
            xml.append("      <g:title>").append(HtmlUtils.htmlEscape(title)).append("</g:title>\n");
            xml.append("      <g:description>").append(HtmlUtils.htmlEscape(description)).append("</g:description>\n");
            xml.append("      <g:link>").append(HtmlUtils.htmlEscape(link)).append("</g:link>\n");
            
            if (imageLink != null) {
                xml.append("      <g:image_link>").append(HtmlUtils.htmlEscape(imageLink)).append("</g:image_link>\n");
            }
            
            if (product.getBrand() != null) {
                xml.append("      <g:brand>").append(HtmlUtils.htmlEscape(product.getBrand().getBrandName())).append("</g:brand>\n");
            }
            
            xml.append("      <g:condition>new</g:condition>\n");
            
            if (product.getProductStock() > 0) {
                xml.append("      <g:availability>in_stock</g:availability>\n");
            } else {
                xml.append("      <g:availability>out_of_stock</g:availability>\n");
            }
            
            if (product.getProductPrice() != null) {
                xml.append("      <g:price>").append(product.getProductPrice()).append(" INR</g:price>\n");
            }
            
            xml.append("    </item>\n");
        }

        xml.append("  </channel>\n");
        xml.append("</rss>");

        return xml.toString();
    }
}
