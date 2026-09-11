/**
 * HTTP adapters under {@code /api/v1}. Controllers stay thin: validate path/body,
 * call a service, return DTOs.
 *
 * <p>Public (no login): inquiries, taxonomy, campaigns, meta, buyer-products,
 * finished-goods list. Staff/admin APIs require HTTP Basic via the in-app form —
 * responses must not send {@code WWW-Authenticate} (that would pop a browser login
 * over the visitor URL).
 */
package com.sarv.exhibitionportal.api;
