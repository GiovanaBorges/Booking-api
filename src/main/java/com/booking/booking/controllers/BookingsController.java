package com.booking.booking.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.DTO.BookingsRequestDTO;
import com.booking.booking.DTO.BookingsResponseDTO;
import com.booking.booking.services.BookingsServices;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/bookings")
public class BookingsController {
    
    @Autowired
    private BookingsServices bookingsServices;

    @PostMapping("/register")
    public ResponseEntity<BookingsResponseDTO> bookingsRegister(@RequestBody BookingsRequestDTO requestDTO,
        @RequestHeader("Idempotency-Key") String key) {
        return ResponseEntity.ok().body(bookingsServices.saveBooking(requestDTO, key));
    }

    @GetMapping("/all")
    public ResponseEntity<List<BookingsResponseDTO>> getAllBookings() {
        return ResponseEntity.ok().body(bookingsServices.getAllBookings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingsResponseDTO> getBookingsByID(@PathVariable Long id) {
        return ResponseEntity.ok().body(bookingsServices.getBookingById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BookingsResponseDTO> deleteBookings(@PathVariable Long id){
        return ResponseEntity.ok().body(bookingsServices.deleteBooking(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<BookingsResponseDTO> updateBookings(@PathVariable Long id, @RequestBody BookingsRequestDTO bookingRequestDTO) {
        return ResponseEntity.ok().body(bookingsServices.updateBooking(id, bookingRequestDTO));
    }
    
}
