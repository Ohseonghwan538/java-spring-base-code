package com.map.egis.controller;

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

    // 1. 이벤트 시작
    @PostMapping("/group/start")
    public ResponseEntity<Map<String, Object>> startGroup(@RequestParam("userId") String userId) {
        Long groupId = imageService.startEventGroup(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("groupId", groupId);
        return ResponseEntity.ok(response);
    }

    // 2. 이미지 파일 업로드 + 메타데이터/공간정보 저장
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("groupId") Long groupId,
            @RequestParam("originalFile") MultipartFile originalFile,
            @RequestParam(value = "compressedFile", required = false) MultipartFile compressedFile,
            @RequestParam("latitude") BigDecimal latitude,
            @RequestParam("longitude") BigDecimal longitude,
            @RequestParam(value = "altitude", required = false) BigDecimal altitude,
            @RequestParam(value = "takenAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime takenAt
    ) {
        Long imageId = imageService.saveImage(
                groupId,
                originalFile,
                compressedFile,
                latitude,
                longitude,
                altitude,
                takenAt
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("imageId", imageId);
        return ResponseEntity.ok(response);
    }

    // 3. 이벤트 종료
    @PostMapping("/group/end")
    public ResponseEntity<Map<String, Object>> endGroup(@RequestParam("groupId") Long groupId) {
        imageService.endEventGroup(groupId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("groupId", groupId);
        return ResponseEntity.ok(response);
    }
}