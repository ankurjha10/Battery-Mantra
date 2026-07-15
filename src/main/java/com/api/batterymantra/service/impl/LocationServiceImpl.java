package com.api.batterymantra.service.impl;

import com.api.batterymantra.dto.location.*;
import com.api.batterymantra.entity.City;
import com.api.batterymantra.entity.Pincode;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.CityRepository;
import com.api.batterymantra.repository.PincodeRepository;
import com.api.batterymantra.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final CityRepository cityRepository;
    private final PincodeRepository pincodeRepository;

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
                .pincodeCount(city.getPincodes() != null ? city.getPincodes().size() : 0)
                .build();
    }

    private PincodeDto mapToPincodeDto(Pincode pincode) {
        return PincodeDto.builder()
                .pincodeId(pincode.getPincodeId())
                .code(pincode.getCode())
                .build();
    }
}
