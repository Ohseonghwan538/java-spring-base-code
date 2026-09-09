package com.map.egis.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ImageMeta {
    private Long imageId;
    private Long groupId;
    private String originalFilePath;
    private String compressedFilePath;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal altitude;
    private LocalDateTime takenAt;
    private LocalDateTime createdAt;
}