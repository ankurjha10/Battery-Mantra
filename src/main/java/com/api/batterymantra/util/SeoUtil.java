package com.api.batterymantra.util;

import com.api.batterymantra.entity.City;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.SeoMetadata;

public class SeoUtil {

    public static SeoMetadata resolveSeo(SeoMetadata raw, City city, Product p) {
        if (raw == null) return null;
        SeoMetadata resolved = new SeoMetadata();
        
        String cityName = city != null ? city.getCityName() : "";
        String deliveryTime = ""; 
        String warrantyRange = ""; 
        String priceRange = p != null && p.getProductPrice() != null ? String.valueOf(p.getProductPrice()) : "";

        java.util.function.Function<String, String> replace = (text) -> {
            if (text == null) return null;
            return text.replace("city_name", cityName)
                       .replace("delivery_time", deliveryTime)
                       .replace("warranty_range", warrantyRange)
                       .replace("price_range", priceRange);
        };

        String effectiveTitle = (raw.getMetaTitleCity() != null && !raw.getMetaTitleCity().isEmpty() && city != null) 
                ? raw.getMetaTitleCity() : raw.getMetaTitle();
        resolved.setMetaTitle(replace.apply(effectiveTitle));

        String effectiveDesc = (raw.getMetaDescriptionCity() != null && !raw.getMetaDescriptionCity().isEmpty() && city != null) 
                ? raw.getMetaDescriptionCity() : raw.getMetaDescription();
        resolved.setMetaDescription(replace.apply(effectiveDesc));

        String effectiveKeywords = (raw.getMetaKeywordsCity() != null && !raw.getMetaKeywordsCity().isEmpty() && city != null) 
                ? raw.getMetaKeywordsCity() : raw.getMetaKeywords();
        resolved.setMetaKeywords(replace.apply(effectiveKeywords));

        String effectiveOgTitle = (raw.getOgTitleCity() != null && !raw.getOgTitleCity().isEmpty() && city != null) 
                ? raw.getOgTitleCity() : raw.getOgTitle();
        resolved.setOgTitle(replace.apply(effectiveOgTitle));

        String effectiveOgDesc = (raw.getOgDescriptionCity() != null && !raw.getOgDescriptionCity().isEmpty() && city != null) 
                ? raw.getOgDescriptionCity() : raw.getOgDescription();
        resolved.setOgDescription(replace.apply(effectiveOgDesc));

        resolved.setSlug(replace.apply(raw.getSlug()));
        resolved.setCanonicalUrl(replace.apply(raw.getCanonicalUrl()));

        return resolved;
    }
}
