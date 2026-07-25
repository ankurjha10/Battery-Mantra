package com.api.batterymantra.util;

import java.util.function.Function;

import com.api.batterymantra.entity.City;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.SeoMetadata;

public class SeoUtil {

    public static SeoMetadata resolveSeo(SeoMetadata raw, City city, Product p) {
        if (raw == null) raw = new SeoMetadata();
        SeoMetadata resolved = new SeoMetadata();
        
        String cityName = city != null ? city.getCityName() : "";
        String deliveryTime = "2-4 Hours"; 
        String warrantyRange = ""; 
        String priceRange = p != null && p.getProductPrice() != null ? String.valueOf(p.getProductPrice()) : "";
        String productName = p != null && p.getProductName() != null ? p.getProductName() : "";
        String brandName = p != null && p.getBrand() != null ? p.getBrand().getBrandName() : "";
        String categoryName = p != null && p.getProductCategory() != null ? p.getProductCategory().getCategoryName() : "";

        Function<String, String> replace = (text) -> {
            if (text == null) return null;
            return text.replace("{product_name}", productName).replace("product_name", productName)
                       .replace("{brand_name}", brandName).replace("brand_name", brandName)
                       .replace("{category_name}", categoryName).replace("category_name", categoryName)
                       .replace("{city_name}", cityName).replace("city_name", cityName)
                       .replace("{delivery_time}", deliveryTime).replace("delivery_time", deliveryTime)
                       .replace("{warranty_range}", warrantyRange).replace("warranty_range", warrantyRange)
                       .replace("{price_range}", priceRange).replace("price_range", priceRange);
        };

        String effectiveTitle = (raw.getMetaTitleCity() != null && !raw.getMetaTitleCity().isEmpty() && city != null) 
                ? raw.getMetaTitleCity() : raw.getMetaTitle();
        if (effectiveTitle == null || effectiveTitle.trim().isEmpty()) {
            effectiveTitle = city != null && !cityName.isEmpty()
                ? "Buy " + productName + " Price in " + cityName + " | Battery Mantra"
                : "Buy " + productName + " at Best Price Online | Battery Mantra";
        }
        resolved.setMetaTitle(replace.apply(effectiveTitle));

        String effectiveDesc = (raw.getMetaDescriptionCity() != null && !raw.getMetaDescriptionCity().isEmpty() && city != null) 
                ? raw.getMetaDescriptionCity() : raw.getMetaDescription();
        if (effectiveDesc == null || effectiveDesc.trim().isEmpty()) {
            effectiveDesc = city != null && !cityName.isEmpty()
                ? "Buy " + productName + " in " + cityName + " at best price with 55 Min Express Delivery & free installation. 100% genuine warranty."
                : "Buy " + productName + " at guaranteed lowest price with free installation & cash on delivery across India. 100% genuine brand warranty.";
        }
        resolved.setMetaDescription(replace.apply(effectiveDesc));

        String effectiveKeywords = (raw.getMetaKeywordsCity() != null && !raw.getMetaKeywordsCity().isEmpty() && city != null) 
                ? raw.getMetaKeywordsCity() : raw.getMetaKeywords();
        if (effectiveKeywords == null || effectiveKeywords.trim().isEmpty()) {
            effectiveKeywords = productName + ", " + productName + " price, buy " + productName + " online, " + brandName + " " + categoryName;
        }
        resolved.setMetaKeywords(replace.apply(effectiveKeywords));

        String effectiveOgTitle = (raw.getOgTitleCity() != null && !raw.getOgTitleCity().isEmpty() && city != null) 
                ? raw.getOgTitleCity() : raw.getOgTitle();
        if (effectiveOgTitle == null || effectiveOgTitle.trim().isEmpty()) {
            effectiveOgTitle = resolved.getMetaTitle();
        }
        resolved.setOgTitle(replace.apply(effectiveOgTitle));

        String effectiveOgDesc = (raw.getOgDescriptionCity() != null && !raw.getOgDescriptionCity().isEmpty() && city != null) 
                ? raw.getOgDescriptionCity() : raw.getOgDescription();
        if (effectiveOgDesc == null || effectiveOgDesc.trim().isEmpty()) {
            effectiveOgDesc = resolved.getMetaDescription();
        }
        resolved.setOgDescription(replace.apply(effectiveOgDesc));

        resolved.setSlug(replace.apply(raw.getSlug()));
        resolved.setCanonicalUrl(replace.apply(raw.getCanonicalUrl()));

        // Preserve raw fields for admin edit
        resolved.setMetaTitleCity(raw.getMetaTitleCity());
        resolved.setMetaDescriptionCity(raw.getMetaDescriptionCity());
        resolved.setMetaKeywordsCity(raw.getMetaKeywordsCity());
        resolved.setOgTitleCity(raw.getOgTitleCity());
        resolved.setOgDescriptionCity(raw.getOgDescriptionCity());

        return resolved;
    }
}
