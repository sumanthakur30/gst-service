package com.shopmanagement.gstservice.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.api.GstApi.GstRegistrationResponse;
import com.shopmanagement.gstservice.api.GstApi.GstRegistrationUpsert;
import com.shopmanagement.gstservice.service.GstRegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gst/registrations")
public class GstRegistrationController {

    private final GstRegistrationService gstRegistrationService;

    public GstRegistrationController(GstRegistrationService gstRegistrationService) {
        this.gstRegistrationService = gstRegistrationService;
    }

    @GetMapping
    public List<GstRegistrationResponse> list() {
        return gstRegistrationService.list();
    }

    @GetMapping("/{id}")
    public GstRegistrationResponse get(@PathVariable Long id) {
        return gstRegistrationService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GstRegistrationResponse create(@Valid @RequestBody GstRegistrationUpsert body) {
        return gstRegistrationService.create(body);
    }
}
