package com.shopmanagement.gstservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GstTaxApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void calculate_requiresTenantHeader() throws Exception {
        mockMvc.perform(post("/api/v1/gst/tax/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applyGst":true,"sellerStateCode":"29","customerStateCode":"29","lines":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculate_withTenant_returnsTotals() throws Exception {
        mockMvc.perform(post("/api/v1/gst/tax/calculate")
                        .header("X-Tenant-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applyGst": true,
                                  "sellerStateCode": "29",
                                  "customerStateCode": "29",
                                  "headerDiscount": 0,
                                  "lines": [
                                    {"lineNo":1,"quantity":1,"unitPrice":118,"gstPercent":18}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gstEnabled").value(true))
                .andExpect(jsonPath("$.totalAmount").isNumber());
    }

    @Test
    void preview_enterpriseRetail_withTenant_returnsTotals() throws Exception {
        mockMvc.perform(post("/api/v1/gst/tax/preview")
                        .header("X-Tenant-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applyGst": true,
                                  "sellerStateCode": "29",
                                  "customerStateCode": "29",
                                  "headerDiscount": 0,
                                  "lines": [
                                    {
                                      "lineNo": 1,
                                      "quantity": 1,
                                      "unitPrice": 118,
                                      "gstPercent": 18,
                                      "taxInclusive": true
                                    }
                                  ],
                                  "businessType": "RETAIL",
                                  "customerGstType": "UNREGISTERED",
                                  "pricingMode": "INCLUSIVE",
                                  "discountBeforeTax": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gstEnabled").value(true))
                .andExpect(jsonPath("$.totalAmount").isNumber());
    }

    @Test
    void gstinValidate() throws Exception {
        mockMvc.perform(post("/api/v1/gst/gstin/validate")
                        .header("X-Tenant-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gstin\":\"29AABCU9603R1ZM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validFormat").value(true))
                .andExpect(jsonPath("$.stateCode").value("29"));
    }
}
