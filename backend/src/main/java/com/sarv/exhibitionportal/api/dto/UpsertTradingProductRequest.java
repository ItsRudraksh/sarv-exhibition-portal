package com.sarv.exhibitionportal.api.dto;

/** Create/update a trading product name and listed_for_buyers flag. */
public record UpsertTradingProductRequest(String name, Boolean listedForBuyers) {}
