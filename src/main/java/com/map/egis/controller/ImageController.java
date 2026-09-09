package com.map.egis.controller;

import com.map.egis.domain.ImageMeta;
import com.map.egis.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/group/start")
    public ResponseEntity<Map<String, Object>> startGroup(@RequestParam("userId") String userId) {
        Long groupId = imageService.startEventGroup(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("groupId", groupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("groupId") Long groupId,
            @RequestParam("originalFile") MultipartFile originalFile,
            @RequestParam(value = "latitude", required = false) BigDecimal latitude,
            @RequestParam(value = "longitude", required = false) BigDecimal longitude,
            @RequestParam(value = "altitude", required = false) BigDecimal altitude,
            @RequestParam(value = "takenAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime takenAt
    ) {
        // 1. Service에서 ImageMeta 객체를 전달받음 (Step 1에서 saveImage 반환타입 변경 필요)
        ImageMeta imageMeta = imageService.saveImage(
                groupId,
                originalFile,
                latitude,
                longitude,
                altitude,
                takenAt
        );

        // 2. 프론트엔드(main.js)에서 필요한 정보를 Map에 저장
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("imageId", imageMeta.getImageId());
        response.put("compressedFilePath", imageMeta.getCompressedFilePath()); // 👈 필수 추가 항목!
        response.put("originalFilePath", imageMeta.getOriginalFilePath());
        response.put("latitude", imageMeta.getLatitude());
        response.put("longitude", imageMeta.getLongitude());
        response.put("altitude", imageMeta.getAltitude());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/group/end")
    public ResponseEntity<Map<String, Object>> endGroup(@RequestParam("groupId") Long groupId) {
        imageService.endEventGroup(groupId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("groupId", groupId);
        return ResponseEntity.ok(response);
    }

    /**
     * 이미지 느낀점(memo) 저장/수정 API
     * 요청 방식: POST /api/images/memo
     * 파라미터: imageId (Long), memo (String)
     */
    @PostMapping("/memo")
    public ResponseEntity<Map<String, Object>> updateMemo(
            @RequestParam("imageId") Long imageId,
            @RequestParam("memo") String memo
    ) {
        imageService.updateImageMemo(imageId, memo);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("imageId", imageId);
        response.put("memo", memo);

        return ResponseEntity.ok(response);
    }
}