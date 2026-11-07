package com.example.volunteer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FavoriteRequest {
    @NotNull(message = "请选择院校")
    private Long universityId;

    private Long majorId;
}
