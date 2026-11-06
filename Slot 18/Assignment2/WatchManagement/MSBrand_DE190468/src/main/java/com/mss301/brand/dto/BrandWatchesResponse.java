package com.mss301.brand.dto;

import java.util.List;

// [TODO-5.5]
public record BrandWatchesResponse(
    Integer brandId, String brandName, long watchCount, List<Integer> watchIds) {}
