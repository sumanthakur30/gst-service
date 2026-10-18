# Promotional supply GST rules

These rules are seeded in `gst_supply_rule` and applied by `GstSupplyDecisionEngine`. A normal user cannot pick a tax treatment. `portalReturnReady` is always false: a books classification is not a GSTR-1 or GSTR-3B portal filing.

| Rule | Effective | Outcome | Basis |
| --- | --- | --- | --- |
| CIRC92_A_FREE_SAMPLE | 7 Mar 2019 | Determined | CGST s.7(1)(a); Circular 92/11/2019-GST para A. Unrelated free samples are not a supply. |
| SEC_17_5_H_ITC | 1 Jul 2017 | Determined | CGST s.17(5)(h); Circular 92 para A(ii). Reverse traced purchase CGST, SGST, IGST, or cess. If the batch has no linked tax, status stays pending and nothing is posted. |
| CIRC92_B_BUY_X_GET_Y | 7 Mar 2019 | Determined | Circular 92 para B. One price for the combined quantity. ITC stays available. |
| SEC_8_MIXED | 1 Jul 2017 | Determined | CGST s.8. A higher-rate free product is taxed at that higher rate on the single price, unless an authorised user marked the line composite. |
| SEC_15_3_A_INVOICE_DISCOUNT | 1 Jul 2017 | Determined | CGST s.15(3)(a). Discount on the original invoice reduces taxable value. |
| SEC_15_3_B_POST_SALE_UNTIL_2025_09_30 | 1 Jul 2017 to 30 Sep 2025 | Determined | s.15(3)(b) as then in force: prior agreement, invoice link, and recipient ITC reversal. Otherwise a commercial credit note. |
| SEC_15_3_B_POST_SALE_FROM_2025_10_01 | 1 Oct 2025 | Review, not activated | Finance Act 2025 provisions were notified by Notification No. 16/2025-Central Tax dated 17 Sep 2025. Published summaries disagree on the amended text of s.15(3)(b). Taxable value is not reduced until a user with MANAGE_GST activates this rule. Commercial credit notes remain available. |
| SCHEDULE_I_RELATED_OR_DISTINCT | 1 Jul 2017 | Determined | Schedule I para 2, s.25(4)/(5), Rule 28, Circular 210/04/2024-GST. Full ITC at the recipient makes the invoice value the open-market value. |
| SCHEDULE_I_EMPLOYEE_GIFT | 1 Jul 2017 | Determined, cap needs review | Gifts to one employee up to Rs 50,000 in a financial year are not a supply. The statute does not define the valuation base, so crossing the cap on either cost or open-market value requires review. |
| SAME_GSTIN_MOVEMENT | 1 Jul 2017 | Determined | Same registration is not a distinct person. Delivery challan. No output GST and no ITC reversal. |
| MR_CUSTODY | 1 Jul 2017 | Determined | Temporary custody is not a disposal. Expense and ITC reversal wait until distribution. |
| IGST_S10_GOODS | 1 Jul 2017 | Determined | IGST s.10. Inter-state when supplier and recipient state codes differ. |

Medicine rates are not hard-coded. Product `gstPercent` and the GST rate schedule remain the rate source. From 22 Sep 2025 the 56th GST Council and Notification 9/2025-Central Tax (Rate) moved most drugs from 12% to 5% and specified life-saving drugs to nil. Other HSN headings can differ.

ITC reversal uses traced purchase tax: line value divided by the AP invoice subtotal, then by the issued quantity over accepted quantity. Example: cost Rs 100 and traced credit Rs 12 reverses Rs 12. The voucher is Dr 5600 Sales promotion, Cr 1200 Stock, and Dr 5600, Cr Input CGST/SGST/IGST. Source type `ITC_REVERSAL` plus the line id prevents a second posting. A return uses `ITC_REVERSAL_RETURN`.

Unresolved: the exact statutory text of section 15(3)(b) from 1 Oct 2025, and the valuation base for the Rs 50,000 employee-gift cap. Both stay in authorised review.
