import http from "k6/http";
import { BASE_URL } from "../config/base.js";

export function createBooking(payload, params) {
  return http.post(`${BASE_URL}/bookings/register`, payload, params);
}

export function getBooking(id, params) {
  return http.get(`${BASE_URL}/bookings/${id}`, params);
}

export function updateBooking(id, payload, params) {
  return http.put(`${BASE_URL}/bookings/update/${id}`, payload, params);
}

export function deleteBooking(id, params) {
  return http.del(`${BASE_URL}/bookings/${id}`, null, params);
}