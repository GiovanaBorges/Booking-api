import http from "k6/http";
import { BASE_URL } from "../config/base.js";

export function getOrCreateUser(params) {
  return http.get(`${BASE_URL}/users/me`, params);
}

export function getUserById(id, params) {
  return http.get(`${BASE_URL}/users/id/${id}`, params);
}

export function updateUser(id, payload, params) {
  return http.put(`${BASE_URL}/users/edit/${id}`, payload, params);
}

export function getUserBySkill(skill, params) {
  return http.get(`${BASE_URL}/users/skill/${skill}`, params);
}