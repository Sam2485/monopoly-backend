package com.BusinessGame.Vyapar.controller;

import com.BusinessGame.Vyapar.common.exception.PropertyNotFoundException;
import com.BusinessGame.Vyapar.config.model.PropertyConfig;
import com.BusinessGame.Vyapar.dto.ApiResponse;
import com.BusinessGame.Vyapar.service.JsonLoaderService;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/v1/properties")
public class PropertyController {

    private final JsonLoaderService jsonLoaderService;

    public PropertyController(JsonLoaderService jsonLoaderService) {
        this.jsonLoaderService = jsonLoaderService;
    }

    @GetMapping("/{propertyId}")
    public ApiResponse<PropertyConfig> getPropertyDetails(@PathVariable Integer propertyId) {
        PropertyConfig config = jsonLoaderService.getPropertyConfig(propertyId);
        if (config == null) {
            throw new PropertyNotFoundException("Property not found with ID: " + propertyId);
        }
        return ApiResponse.success(config, "Loaded property details");
    }

    @GetMapping
    public ApiResponse<Collection<PropertyConfig>> listAllProperties() {
        Collection<PropertyConfig> properties = jsonLoaderService.getAllPropertyConfigs();
        return ApiResponse.success(properties, "Loaded all properties");
    }
}
