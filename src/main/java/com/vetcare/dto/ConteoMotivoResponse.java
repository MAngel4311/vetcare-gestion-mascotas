package com.vetcare.dto;

import com.vetcare.model.enums.MotivoCita;

public record ConteoMotivoResponse(MotivoCita motivo, long total) {
}
