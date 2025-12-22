package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.DTO.ProviderAvailabilityRequestDTO;
import com.booking.booking.DTO.ProviderAvailabilityResponseDTO;
import com.booking.booking.services.ProviderAvailabilityservices;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/provider-availability")
public class ProviderAvailabilityController {

    @Autowired
    private ProviderAvailabilityservices services;
    
    @PostMapping("/register")
    public ResponseEntity<ProviderAvailabilityResponseDTO> registerProviderAvailability(
        @RequestBody ProviderAvailabilityRequestDTO request,
        @RequestHeader("Idempotency-Key") String key) {
        return ResponseEntity.ok().body(services.saveProviderAvailability(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProviderAvailabilityResponseDTO> updateProvider(@PathVariable Long id, @RequestBody ProviderAvailabilityRequestDTO requestDTO) {
        ProviderAvailabilityResponseDTO response = services.updateProvider(id,requestDTO);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ProviderAvailabilityResponseDTO> deleteProviderById(@PathVariable Long id) {
        return ResponseEntity.ok().body(services.deleteProviderById(id));
    }

    @GetMapping("/allproviders")
    public ResponseEntity<List<ProviderAvailabilityResponseDTO>> getAllProviders() {
        List<ProviderAvailabilityResponseDTO> response = services.getAllProvider();
        return ResponseEntity.ok().body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProviderAvailabilityResponseDTO> findProviderAvailableById(@PathVariable Long id) {
        return ResponseEntity.ok().body(services.findProviderById(id));
    }


    
    
}
