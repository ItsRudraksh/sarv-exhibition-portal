/**
 * Security, CORS, env-file loading, and production fail-closed checks.
 *
 * <p>Visitor URLs and card OCR {@code /tessdata/**} must stay anonymous.
 * Profile {@code prod} refuses known POC secrets ({@link ProductionStartupGuard}).
 */
package com.sarv.exhibitionportal.config;
