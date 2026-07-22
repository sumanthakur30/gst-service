package com.shopmanagement.gstservice.compliance;

import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;

public interface EinvoiceProvider {
    IrnResult generate(TaxDocumentSnapshot snapshot);

    /** Cancel an existing IRN. Default unsupported — mocks/HTTP override. */
    default CancelResult cancel(String irn, String reason) {
        throw new UnsupportedOperationException("IRN cancel not supported by this provider");
    }
}
