package com.mss301.brand.dto;
import lombok.Builder;
// [TODO-5.5]
@Builder public record BrandResponse(Integer brandId,String brandName,String countryOfOrigin,long watchCount) {}
