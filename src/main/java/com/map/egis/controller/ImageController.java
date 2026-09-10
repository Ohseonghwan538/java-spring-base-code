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
import java.util.List;
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/group/start")
    public ResponseEntity<ImageApiResponse.Group> startGroup(@RequestParam("userId") String userId) {
        Long groupId = imageService.startEventGroup(userId);
        return ResponseEntity.ok(ImageApiResponse.Group.success(groupId));
    }

    @PostMapping("/upload")
    public ResponseEntity<ImageApiResponse.Upload> uploadImage(
            @RequestParam("groupId") Long groupId,
            @RequestParam("originalFile") MultipartFile originalFile,
            @RequestParam(value = "latitude", required = false) BigDecimal latitude,
            @RequestParam(value = "longitude", required = false) BigDecimal longitude,
            @RequestParam(value = "altitude", required = false) BigDecimal altitude,
            @RequestParam(value = "takenAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime takenAt
    ) {
        ImageMeta imageMeta = imageService.saveImage(
                groupId,
                originalFile,
                latitude,
                longitude,
                altitude,
                takenAt
        );

        return ResponseEntity.ok(ImageApiResponse.Upload.success(imageMeta));
    }

    @PostMapping("/group/end")
    public ResponseEntity<ImageApiResponse.Group> endGroup(@RequestParam("groupId") Long groupId) {
        imageService.endEventGroup(groupId);
        return ResponseEntity.ok(ImageApiResponse.Group.success(groupId));
    }

    /**
     * 이미지 느낀점(memo) 저장/수정 API
     * 요청 방식: POST /api/images/memo
     * 파라미터: imageId (Long), memo (String)
     */
    @PostMapping("/memo")
    public ResponseEntity<ImageApiResponse.Memo> updateMemo(
            @RequestParam("imageId") Long imageId,
            @RequestParam("memo") String memo
    ) {
        imageService.updateImageMemo(imageId, memo);

        return ResponseEntity.ok(ImageApiResponse.Memo.success(imageId, memo));
    }

    @GetMapping
    public ResponseEntity<ImageApiResponse.Images> loadImages(
            @RequestParam String userId,
            @RequestParam Long groupId
    ) {
        List<ImageMeta> images = imageService.findImages(userId, groupId);
        return ResponseEntity.ok(ImageApiResponse.Images.success(groupId, images));
    }
}
