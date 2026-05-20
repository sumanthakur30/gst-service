package com.shopmanagement.gstservice.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.api.GstApi.GstinValidateRequest;
import com.shopmanagement.gstservice.api.GstApi.GstinValidateResponse;
import com.shopmanagement.gstservice.support.GstinValidator;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gst/gstin")
public class GstinController {

    @PostMapping("/validate")
    public GstinValidateResponse validate(@Valid @RequestBody GstinValidateRequest request) {
        return GstinValidator.validate(request.gstin());
    }
}
