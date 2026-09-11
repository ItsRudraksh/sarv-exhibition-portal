/**
 * JSON records shared by visitor, staff, and admin clients.
 *
 * <p>{@link InquiryDraftDto} is the visitor draft contract (camelCase). Compact
 * constructors exist so tests and older payloads still compile when optional
 * fields (Other flags, attachments) are omitted. Jackson binds the canonical
 * record; null strings are normalised to empty in compact constructors.
 *
 * <p>Do not put card QR text on visitor GET — {@code cardQrPayloadInternal} is
 * redacted in {@code InquiryService}.
 */
package com.sarv.exhibitionportal.api.dto;
