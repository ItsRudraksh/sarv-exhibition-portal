/**
 * Submit-time and draft persistence for visitor inquiries.
 *
 * <p>{@link InquiryService} is the facade (create, PATCH, contact confirm, submit).
 * {@link InquiryRules} is the product contract — keep it aligned with
 * {@code frontend/src/features/inquiry/validation.ts}.
 * {@link InquiryRepository} maps the draft onto {@code inquiries} plus route
 * extension tables ({@code supplier_inquiries} / {@code purchase_inquiries}).
 */
package com.sarv.exhibitionportal.inquiry;
