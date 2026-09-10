package com.map.egis.controller;

import com.map.egis.domain.ImageMeta;

import java.math.BigDecimal;
import java.util.List;

/** Response payloads for the image API. Field names intentionally match the existing API contract. */
public final class ImageApiResponse {

    private static final String SUCCESS = "SUCCESS";

    private ImageApiResponse() {
    }

    public record Group(String status, Long groupId) {
        static Group success(Long groupId) {
            return new Group(SUCCESS, groupId);
        }
    }

    public record Upload(
            String status,
            Long imageId,
            String compressedFilePath,
            String originalFilePath,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal altitude
    ) {
        static Upload success(ImageMeta imageMeta) {
            return new Upload(
                    SUCCESS,
                    imageMeta.getImageId(),
                    imageMeta.getCompressedFilePath(),
                    imageMeta.getOriginalFilePath(),
                    imageMeta.getLatitude(),
                    imageMeta.getLongitude(),
                    imageMeta.getAltitude()
            );
        }
    }

    public record Memo(String status, Long imageId, String memo) {
        static Memo success(Long imageId, String memo) {
            return new Memo(SUCCESS, imageId, memo);
        }
    }

    public record Images(String status, Long groupId, List<ImageMeta> images) {
        static Images success(Long groupId, List<ImageMeta> images) {
            return new Images(SUCCESS, groupId, images);
        }
    }
}
