package com.booking.booking.services.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;

@Service
public class UserResolver {

    @Autowired
    private UsersRepository usersRepository;

    public Users resolveCustomerById(Long id) {
        return usersRepository.findById(id)
            .orElseThrow(() -> new ApiException("CUSTOMER NOT FOUND", HttpStatus.NOT_FOUND));

    }
    
    public Users resolveProviderById(Long id) {
        return usersRepository.findById(id)
            .orElseThrow(() -> new ApiException("PROVIDER NOT FOUND", HttpStatus.NOT_FOUND));
}
}