package com.map.egis.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageMeta {
    private Long imageId;
    private Long groupId;
    private String originalFilePath;
    private String compressedFilePath;
    private BigDecimal latitude;   // 위치 누락 시 null 가능
    private BigDecimal longitude;  // 위치 누락 시 null 가능
    private BigDecimal altitude;   // 위치 누락 시 null 가능
    private String memo;
    private LocalDateTime takenAt;
    private LocalDateTime createdAt;
}