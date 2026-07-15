package com.api.batterymantra.service;

import com.api.batterymantra.dto.location.*;

import java.util.List;
import java.util.UUID;

public interface LocationService {
    CityDto createCity(CreateCityRequest request);
    List<CityDto> getAllCities();
    CityDto updateCity(UUID cityId, UpdateCityRequest request);
    void deleteCity(UUID cityId);

    List<PincodeDto> addPincodes(UUID cityId, AddPincodeRequest request);
    List<PincodeDto> getPincodesByCity(UUID cityId);
    void deletePincode(UUID pincodeId);
}
