/**
 * Internal supplier review queue and buyer marketing leads.
 *
 * <p>Approve / reject / needs-info are staff actions. Creating or updating an
 * enterprise vendor happens only on explicit <strong>Add to production</strong>
 * (outbox {@code VENDOR_UPSERT}). AI must never approve.
 */
package com.sarv.exhibitionportal.review;
