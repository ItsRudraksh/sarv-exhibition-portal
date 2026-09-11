/**
 * Reliable delivery stubs. Buyer submit enqueues {@code MARKETING_LEAD};
 * Add to production enqueues {@code VENDOR_UPSERT}. The worker writes local JSON
 * until real CRM/vendor APIs exist. Retries; never deletes the inquiry on failure.
 */
package com.sarv.exhibitionportal.outbox;
