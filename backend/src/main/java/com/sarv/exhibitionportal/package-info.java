/**
 * Sarv Biolabs exhibition portal API. Spring Boot 3, Java 17, JDBC + Flyway.
 *
 * <p>Visitor sell/buy drafts, staff review, and admin catalogue tagging share this
 * process. Product rules: {@code specs/PLATFORM_CONTEXT.md}. Module map:
 * {@code specs/CODE-WALKTHROUGH.md}.
 *
 * <p>Scheduling ({@code @EnableScheduling}) runs the outbox worker; it must never
 * drop a submitted inquiry if a stub delivery fails.
 */
package com.sarv.exhibitionportal;
