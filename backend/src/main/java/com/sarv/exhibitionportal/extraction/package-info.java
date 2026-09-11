/**
 * Card-assist extraction. Server ZXing reads QR from uploaded card images.
 * Proposed fields are reviewable; the visitor GET never includes the raw QR string.
 * Client Tesseract/jsQR is a parallel assist — see {@code frontend/.../cardOcr.ts}.
 */
package com.sarv.exhibitionportal.extraction;
