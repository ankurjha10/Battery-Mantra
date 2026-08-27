package com.api.batterymantra.service.impl;

import com.api.batterymantra.dto.location.*;
import com.api.batterymantra.entity.City;
import com.api.batterymantra.entity.Pincode;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.CityRepository;
import com.api.batterymantra.repository.PincodeRepository;
import com.api.batterymantra.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final CityRepository cityRepository;
    private final PincodeRepository pincodeRepository;

    private static final String INDIA_POST_API_URL = "https://api.postalpincode.in/pincode/";

    @Override
    @Transactional
    public CityDto createCity(CreateCityRequest request) {
        if (cityRepository.existsByCityNameIgnoreCase(request.getCityName())) {
            throw new IllegalArgumentException("City with name " + request.getCityName() + " already exists.");
        }

        City city = new City();
        city.setCityName(request.getCityName());
        city.setStateName(request.getStateName());
        city.setCityImage(request.getCityImage());
        city.setIsPopular(request.getIsPopular() != null ? request.getIsPopular() : false);
        city.setIsCodAvailable(request.getIsCodAvailable() != null ? request.getIsCodAvailable() : false);
        city.setIsExchangeAvailable(request.getIsExchangeAvailable() != null ? request.getIsExchangeAvailable() : false);
        city.setDeliveryCharge(request.getDeliveryCharge() != null ? request.getDeliveryCharge() : 0.0);
        city.setOriginalDeliveryCharge(request.getOriginalDeliveryCharge() != null ? request.getOriginalDeliveryCharge() : 40.0);

        City savedCity = cityRepository.save(city);
        return mapToCityDto(savedCity);
    }

    @Override
    public List<CityDto> getAllCities() {
        return cityRepository.findAll().stream()
                .map(this::mapToCityDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CityDto updateCity(UUID cityId, UpdateCityRequest request) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + cityId));

        if (request.getCityName() != null && !request.getCityName().equalsIgnoreCase(city.getCityName())) {
            if (cityRepository.existsByCityNameIgnoreCase(request.getCityName())) {
                throw new IllegalArgumentException("City with name " + request.getCityName() + " already exists.");
            }
            city.setCityName(request.getCityName());
        }

        if (request.getStateName() != null) city.setStateName(request.getStateName());
        if (request.getCityImage() != null) city.setCityImage(request.getCityImage());
        if (request.getIsPopular() != null) city.setIsPopular(request.getIsPopular());
        if (request.getIsCodAvailable() != null) city.setIsCodAvailable(request.getIsCodAvailable());
        if (request.getIsExchangeAvailable() != null) city.setIsExchangeAvailable(request.getIsExchangeAvailable());
        if (request.getDeliveryCharge() != null) city.setDeliveryCharge(request.getDeliveryCharge());
        if (request.getOriginalDeliveryCharge() != null) city.setOriginalDeliveryCharge(request.getOriginalDeliveryCharge());

        City updatedCity = cityRepository.save(city);
        return mapToCityDto(updatedCity);
    }

    @Override
    @Transactional
    public void deleteCity(UUID cityId) {
        if (!cityRepository.existsById(cityId)) {
            throw new ResourceNotFoundException("City not found with id: " + cityId);
        }
        cityRepository.deleteById(cityId);
    }

    @Override
    @Transactional
    public List<PincodeDto> addPincodes(UUID cityId, AddPincodeRequest request) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + cityId));

        List<Pincode> newPincodes = new ArrayList<>();
        List<String> uniqueCodes = request.getCodes().stream().distinct().collect(Collectors.toList());
        for (String code : uniqueCodes) {
            if (pincodeRepository.existsByCode(code)) {
                throw new IllegalArgumentException("Pincode " + code + " already exists in the system.");
            }
            Pincode pincode = new Pincode();
            pincode.setCode(code);
            pincode.setCity(city);
            newPincodes.add(pincode);
        }

        List<Pincode> savedPincodes = pincodeRepository.saveAll(newPincodes);
        return savedPincodes.stream().map(this::mapToPincodeDto).collect(Collectors.toList());
    }

    @Override
    public List<PincodeDto> getPincodesByCity(UUID cityId) {
        if (!cityRepository.existsById(cityId)) {
            throw new ResourceNotFoundException("City not found with id: " + cityId);
        }
        return pincodeRepository.findByCity_CityId(cityId).stream()
                .map(this::mapToPincodeDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePincode(UUID pincodeId) {
        if (!pincodeRepository.existsById(pincodeId)) {
            throw new ResourceNotFoundException("Pincode not found with id: " + pincodeId);
        }
        pincodeRepository.deleteById(pincodeId);
    }

    private CityDto mapToCityDto(City city) {
        return CityDto.builder()
                .cityId(city.getCityId())
                .cityName(city.getCityName())
                .stateName(city.getStateName())
                .cityImage(city.getCityImage())
                .isPopular(city.getIsPopular())
                .isCodAvailable(city.getIsCodAvailable())
                .isExchangeAvailable(city.getIsExchangeAvailable())
                .deliveryCharge(city.getDeliveryCharge() != null ? city.getDeliveryCharge() : 0.0)
                .originalDeliveryCharge(city.getOriginalDeliveryCharge() != null ? city.getOriginalDeliveryCharge() : 40.0)
                .pincodeCount(city.getPincodes() != null ? city.getPincodes().size() : 0)
                .build();
    }

    private PincodeDto mapToPincodeDto(Pincode pincode) {
        return PincodeDto.builder()
                .pincodeId(pincode.getPincodeId())
                .code(pincode.getCode())
                .build();
    }

    @Override
    public PincodeCheckResponse checkPincode(String code) {
        // Step 1: Check if pincode exists directly in our DB (area-specific match)
        Optional<Pincode> dbPincode = pincodeRepository.findByCode(code);
        if (dbPincode.isPresent()) {
            return PincodeCheckResponse.builder()
                    .isServiceable(true)
                    .city(mapToCityDto(dbPincode.get().getCity()))
                    .build();
        }

        // Step 2: Pincode not in DB — call India Post API to resolve location
        try {
            RestTemplate restTemplate = new RestTemplate();
            IndiaPostApiResponse[] responses = restTemplate.getForObject(
                    INDIA_POST_API_URL + code, IndiaPostApiResponse[].class);

            if (responses == null || responses.length == 0) {
                return buildNotServiceable();
            }

            IndiaPostApiResponse response = responses[0];
            if (!"Success".equalsIgnoreCase(response.getStatus())
                    || response.getPostOffice() == null
                    || response.getPostOffice().isEmpty()) {
                return buildNotServiceable();
            }

            // Step 3 & 4: Match against our registered cities using multiple fields from all PostOffice entries
            List<City> allCities = cityRepository.findAll();
            City matchedCity = null;

            outerLoop:
            for (IndiaPostApiResponse.PostOffice postOffice : response.getPostOffice()) {
                String[] apiFields = {
                        postOffice.getName(),
                        postOffice.getDistrict(),
                        postOffice.getBlock(),
                        postOffice.getRegion(),
                        postOffice.getDivision(),
                        postOffice.getState()
                };

                for (City city : allCities) {
                    String cityNameLower = city.getCityName().toLowerCase();
                    for (String field : apiFields) {
                        if (field != null && field.toLowerCase().contains(cityNameLower)) {
                            matchedCity = city;
                            break outerLoop;
                        }
                    }
                }
            }

            if (matchedCity == null) {
                return buildNotServiceable();
            }

            City city = matchedCity;
            int registeredPincodeCount = city.getPincodes() != null ? city.getPincodes().size() : 0;

            if (registeredPincodeCount == 0) {
                // No specific pincodes registered → full-city delivery is ON
                return PincodeCheckResponse.builder()
                        .isServiceable(true)
                        .city(mapToCityDto(city))
                        .build();
            } else {
                // Admin restricted to specific pincodes, and this one wasn't in DB (step 1 failed)
                return buildNotServiceable();
            }

        } catch (Exception e) {
            log.error("Error calling India Post API for pincode {}: {}", code, e.getMessage());
            return buildNotServiceable();
        }
    }

    private PincodeCheckResponse buildNotServiceable() {
        return PincodeCheckResponse.builder()
                .isServiceable(false)
                .city(null)
                .build();
    }
}

