package com.shopmanagement.gstservice.compliance;

import com.shopmanagement.gstservice.model.EwayBillRequest;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;

public interface EwayBillProvider {
    EwayResult generate(TaxDocumentSnapshot snapshot, EwayBillRequest request);
}
