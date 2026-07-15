package com.shopmanagement.gstservice.compliance;

import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;

public interface EinvoiceProvider {
    IrnResult generate(TaxDocumentSnapshot snapshot);
}
