package com.restaurant.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.search.dto.CreateRestaurantRequest;
import com.restaurant.search.dto.RestaurantResponse;
import com.restaurant.search.exception.RestaurantNotFoundException;
import com.restaurant.search.service.RestaurantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests del controller de Restaurant.
 */
@WebMvcTest(RestaurantController.class)
class RestaurantControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RestaurantService restaurantService;

    @Test
    @DisplayName("GET /restaurants/{id} es publico y devuelve 200")
    void getIsPublic() throws Exception {
        final RestaurantResponse response = new RestaurantResponse(
                "rest-1", "owner", "X", null, "Toluca", "Calle 5", "MEXICAN",
                "MODERATE", 0.0, 0, List.of(), 40, null, null, "ACTIVE", "now", "now");
        when(restaurantService.getRestaurant("rest-1")).thenReturn(response);
        mockMvc.perform(get("/api/v1/restaurants/rest-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value("rest-1"));
    }

    @Test
    @DisplayName("GET /search?city=... devuelve array")
    void searchByCity() throws Exception {
        when(restaurantService.getRestaurantsByCity("Toluca")).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/restaurants/search").param("city", "Toluca"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /restaurants sin JWT -> 401")
    void postWithoutJwt() throws Exception {
        final CreateRestaurantRequest request = new CreateRestaurantRequest(
                "X", "Y", "Toluca", "Calle 5", "MEXICAN", "MODERATE",
                40, List.of(), null, null);
        mockMvc.perform(post("/api/v1/restaurants")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /restaurants con priceRange invalido -> 400")
    void postBadPriceRange() throws Exception {
        final CreateRestaurantRequest request = new CreateRestaurantRequest(
                "X", "Y", "Toluca", "Calle 5", "MEXICAN", "BARATO",
                40, List.of(), null, null);
        mockMvc.perform(post("/api/v1/restaurants")
                        .with(jwt().jwt(token -> token.subject("owner-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /restaurants happy path -> 201")
    void postHappyPath() throws Exception {
        final CreateRestaurantRequest request = new CreateRestaurantRequest(
                "X", "Y", "Toluca", "Calle 5", "MEXICAN", "MODERATE",
                40, List.of(), null, null);
        final RestaurantResponse response = new RestaurantResponse(
                "new", "owner-1", "X", "Y", "Toluca", "Calle 5", "MEXICAN", "MODERATE",
                0.0, 0, List.of(), 40, null, null, "PENDING_APPROVAL", "now", "now");
        when(restaurantService.createRestaurant(anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/restaurants")
                        .with(jwt().jwt(token -> token.subject("owner-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /restaurants/{id} sin JWT -> 401")
    void deleteWithoutJwt() throws Exception {
        mockMvc.perform(delete("/api/v1/restaurants/rest-1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /restaurants/{id} no encontrado -> 404")
    void getNotFound() throws Exception {
        when(restaurantService.getRestaurant("rest-x")).thenThrow(new RestaurantNotFoundException("missing"));
        mockMvc.perform(get("/api/v1/restaurants/rest-x"))
                .andExpect(status().isNotFound());
    }
}
