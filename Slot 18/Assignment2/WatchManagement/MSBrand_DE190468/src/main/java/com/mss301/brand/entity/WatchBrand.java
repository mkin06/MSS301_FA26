package com.mss301.brand.entity;
import jakarta.persistence.*;
import lombok.*;
// [TODO-5.1] DBBrand owns this table; no cross-database entity relationship.
@Entity @Table(name="WatchBrands") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WatchBrand {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="BrandID") private Integer brandId;
    @Column(name="BrandName",length=200) private String brandName;
    @Column(name="CountryOfOrigin",length=50) private String countryOfOrigin;
}
