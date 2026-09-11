/**
 * Staff identity ({@code app_users} + {@code user_roles}) and HTTP Basic user details.
 *
 * <p>Roles: ADMIN, SUPPLIER_REVIEWER, MARKETING, EXPORTER, TAXONOMY_MANAGER.
 * {@link StaffPasswordBootstrap} rotates only seeded {@code poc-*} accounts when
 * {@code EXHIBITION_STAFF_BOOTSTRAP_PASSWORD} is set. Admin-created users keep
 * their own password.
 */
package com.sarv.exhibitionportal.staff;
