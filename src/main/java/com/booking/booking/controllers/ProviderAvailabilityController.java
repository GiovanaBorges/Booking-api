package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.DTO.requests.ProviderAvailabilityRequestDTO;
import com.booking.booking.DTO.responses.ProviderAvailabilityResponseDTO;
import com.booking.booking.services.ProviderAvailabilityservices;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/provideravailability")
public class ProviderAvailabilityController {

    @Autowired
    private ProviderAvailabilityservices services;
    
    @PostMapping("/register")
    public ResponseEntity<ProviderAvailabilityResponseDTO> registerProviderAvailability(
        @RequestBody ProviderAvailabilityRequestDTO request) {
        return ResponseEntity.ok().body(services.saveProviderAvailability(request));
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<ProviderAvailabilityResponseDTO> updateProvider(@PathVariable Long id, @RequestBody ProviderAvailabilityRequestDTO requestDTO) {
        ProviderAvailabilityResponseDTO response = services.updateProvider(id,requestDTO);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<ProviderAvailabilityResponseDTO> deleteProviderById(@PathVariable Long id) {
        return ResponseEntity.ok().body(services.deleteProviderById(id));
    }

    @GetMapping("/allproviders")
    public ResponseEntity<List<ProviderAvailabilityResponseDTO>> getAllProviders() {
        List<ProviderAvailabilityResponseDTO> response = services.getAllProvider();
        return ResponseEntity.ok().body(response);
    }
    
    @GetMapping("/id/{id}")
    public ResponseEntity<ProviderAvailabilityResponseDTO> findProviderAvailableById(@PathVariable Long id) {
        System.out.println("BATEU NO ID: " + id);
        return ResponseEntity.ok().body(services.findProviderById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ProviderAvailabilityResponseDTO>> getMyAvailabilities() {
        return ResponseEntity.ok(services.getMyAvailabilities());
    }

    @GetMapping
    public List<ProviderAvailabilityResponseDTO> getAvailabilityByDate(
            @RequestParam Long providerId,
            @RequestParam String date) {
        return services.getAvailabilityByDate(providerId, date);
    }

    
}
