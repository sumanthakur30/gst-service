package com.shopmanagement.gstservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaxDocumentPhase1IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postDocument_isIdempotent() throws Exception {
        String body = """
                {
                  "sourceService": "order-service",
                  "sourceType": "ORDER",
                  "sourceId": "9001",
                  "documentType": "SALES_RECEIPT",
                  "documentDate": "2026-05-19",
                  "assignInvoiceNumber": false,
                  "tax": {
                    "applyGst": true,
                    "sellerStateCode": "29",
                    "customerStateCode": "29",
                    "lines": [{"lineNo":1,"quantity":1,"unitPrice":118,"gstPercent":18}]
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/gst/documents/post")
                        .header("X-Tenant-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.snapshotHash").isNotEmpty());

        mockMvc.perform(post("/api/v1/gst/documents/post")
                        .header("X-Tenant-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceId").value("9001"));
    }

    @Test
    void branchMap_and_invoiceSeries() throws Exception {
        String createBody = mockMvc.perform(post("/api/v1/gst/registrations")
                        .header("X-Tenant-Id", "102")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "legalName": "Test Pharma",
                                  "gstin": "29AABCU9603R1ZM",
                                  "stateCode": "29",
                                  "registrationType": "REGULAR"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = new ObjectMapper().readTree(createBody);
        long regId = created.get("id").asLong();

        mockMvc.perform(post("/api/v1/gst/branches/map")
                        .header("X-Tenant-Id", "102")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gstRegistrationId": %d,
                                  "shopId": "shop-abc",
                                  "isDefault": true
                                }
                                """.formatted(regId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopId").value("shop-abc"));

        mockMvc.perform(post("/api/v1/gst/invoices/next-number")
                        .header("X-Tenant-Id", "102")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gstRegistrationId": %d,
                                  "documentDate": "2026-05-19"
                                }
                                """.formatted(regId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV/2026-27/000001"));
    }
}
