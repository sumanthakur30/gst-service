package com.shopmanagement.gstservice.compliance;

/** Request to update e-way Part-B (vehicle / transporter details). */
public record PartBRequest(
        String vehicleNo,
        String fromPlace,
        String transDocNo) {
}
