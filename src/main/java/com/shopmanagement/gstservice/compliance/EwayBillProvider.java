package com.shopmanagement.gstservice.compliance;

import com.shopmanagement.gstservice.model.EwayBillRequest;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;

public interface EwayBillProvider {
    EwayResult generate(TaxDocumentSnapshot snapshot, EwayBillRequest request);

    /** Cancel an existing e-way bill. Default unsupported — mocks/HTTP override. */
    default CancelResult cancel(String ewbNo, String reason) {
        throw new UnsupportedOperationException("E-way cancel not supported by this provider");
    }

    /** Update Part-B (vehicle / transporter) on an existing e-way bill. */
    default PartBResult updatePartB(String ewbNo, PartBRequest request) {
        throw new UnsupportedOperationException("E-way Part-B update not supported by this provider");
    }
}
