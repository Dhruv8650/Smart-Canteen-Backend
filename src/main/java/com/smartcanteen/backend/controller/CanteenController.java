package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.response.CanteenResponseDTO;
import com.smartcanteen.backend.entity.Canteen;
import com.smartcanteen.backend.service.CanteenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/canteen")
@RequiredArgsConstructor
public class CanteenController {

    private final CanteenService canteenService;

    @GetMapping
    public ResponseEntity<ApiResponse<CanteenResponseDTO>> getCanteenStatus() {

        Canteen canteen = canteenService.getCanteen();

        // Convert Entity → DTO
        CanteenResponseDTO dto = new CanteenResponseDTO(
                canteen.getStatus().name(), // enum → string
                canteen.getClosingSoonUntil(),
                canteen.isKitchenReady(),
                canteen.isManagerReady()
        );

        return ResponseEntity.ok(
                ApiResponse.<CanteenResponseDTO>builder()
                        .success(true)
                        .message("Canteen status fetched successfully")
                        .data(dto)
                        .build()
        );
    }
}