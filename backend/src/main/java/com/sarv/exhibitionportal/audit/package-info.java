/**
 * Append-only operational audit. Metadata must not contain passwords, card QR
 * payloads, or raw file bytes. Callers pass an entity type + action + JSON-ish map.
 */
package com.sarv.exhibitionportal.audit;
