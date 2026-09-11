/**
 * Append-only consent records (card images, etc.). Never UPDATE a prior
 * GRANT/DECLINE; the latest row by {@code decided_at} is current.
 */
package com.sarv.exhibitionportal.consent;
