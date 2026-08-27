package com.api.batterymantra.dto.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO to deserialize the India Post API response.
 * API: https://api.postalpincode.in/pincode/{code}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndiaPostApiResponse {

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("PostOffice")
    private List<PostOffice> postOffice;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PostOffice {

        @JsonProperty("Name")
        private String name;

        @JsonProperty("District")
        private String district;

        @JsonProperty("Block")
        private String block;

        @JsonProperty("State")
        private String state;

        @JsonProperty("Region")
        private String region;

        @JsonProperty("Division")
        private String division;

        @JsonProperty("Country")
        private String country;

        @JsonProperty("Pincode")
        private String pincode;
    }
}
