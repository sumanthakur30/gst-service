package com.shopmanagement.gstservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.shopmanagement.gstservice.api.GstApi.GstinValidateResponse;
import com.shopmanagement.gstservice.api.GstApi.TaxValidationRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxValidationResponse;
import com.shopmanagement.gstservice.model.CustomerGstType;
import com.shopmanagement.gstservice.repository.HsnSacMasterRepository;
import com.shopmanagement.gstservice.repository.StateCodeMasterRepository;
import com.shopmanagement.gstservice.support.GstinValidator;

@Service
public class GstValidationService {

    private static final Pattern HSN_PATTERN = Pattern.compile("^[0-9]{4,8}$|^[0-9]{6}$");

    private final HsnSacMasterRepository hsnSacMasterRepository;
    private final StateCodeMasterRepository stateCodeMasterRepository;

    public GstValidationService(
            HsnSacMasterRepository hsnSacMasterRepository,
            StateCodeMasterRepository stateCodeMasterRepository) {
        this.hsnSacMasterRepository = hsnSacMasterRepository;
        this.stateCodeMasterRepository = stateCodeMasterRepository;
    }

    public TaxValidationResponse validate(TaxValidationRequest request) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (request.gstin() != null && !request.gstin().isBlank()) {
            GstinValidateResponse gstin = GstinValidator.validate(request.gstin());
            if (!gstin.validFormat()) {
                errors.add(gstin.message());
            } else if (request.stateCode() != null
                    && !request.stateCode().isBlank()
                    && !request.stateCode().equals(gstin.stateCode())) {
                errors.add("GSTIN state code does not match supplied state code");
            }
        }

        if (request.stateCode() != null && !request.stateCode().isBlank()
                && stateCodeMasterRepository.findById(request.stateCode().trim()).isEmpty()) {
            errors.add("Invalid state code: " + request.stateCode());
        }

        if (request.hsnSac() != null && !request.hsnSac().isBlank()) {
            String hsn = request.hsnSac().trim();
            if (!HSN_PATTERN.matcher(hsn).matches()) {
                errors.add("Invalid HSN/SAC format");
            } else if (hsnSacMasterRepository.findByCodeIgnoreCase(hsn).isEmpty()) {
                warnings.add("HSN/SAC not found in master — rate may use default slab");
            }
        }

        if (request.sellerStateCode() != null && request.customerStateCode() != null
                && !request.sellerStateCode().isBlank()
                && request.sellerStateCode().equals(request.customerStateCode().trim())) {
            CustomerGstType type = CustomerGstType.from(request.customerGstType());
            if (type == CustomerGstType.EXPORT) {
                warnings.add("Export customer with same state as seller — verify place of supply");
            }
        }

        return new TaxValidationResponse(errors.isEmpty(), errors, warnings);
    }
}
