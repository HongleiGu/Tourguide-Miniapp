package com.tourguide.backend.api.dto;

/** A tourist's 核销码 plus a scannable QR image (PNG data-URL). */
public record VerifyQr(
        String code,
        String dataUrl) {
}
