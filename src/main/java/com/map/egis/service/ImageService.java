package com.map.egis.service;

import com.map.egis.domain.ImageGroup;
import com.map.egis.domain.ImageMeta;
import com.map.egis.mapper.ImageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageMapper imageMapper;

    @Transactional
    public Long startEventGroup(String userId) {
        ImageGroup group = ImageGroup.builder()
                .userId(userId)
                .startedAt(LocalDateTime.now())
                .build();

        imageMapper.insertImageGroup(group);
        return group.getGroupId();
    }

    @Transactional
    public Long saveImage(
            Long groupId,
            MultipartFile originalFile,
            MultipartFile compressedFile,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal altitude,
            LocalDateTime takenAt
    ) {
        // 일자별 디렉터리 파티셔닝 (yyyy/MM/dd)
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String projectRoot = new File("").getAbsolutePath();

        String origDir = projectRoot + "/storage/original/" + datePath;
        String compDir = projectRoot + "/storage/compressed/" + datePath;

        new File(origDir).mkdirs();
        new File(compDir).mkdirs();

        // 파일명 UUID 처리
        String origUUID = UUID.randomUUID() + "_" + originalFile.getOriginalFilename();
        String compUUID = UUID.randomUUID() + "_" + (compressedFile != null && !compressedFile.isEmpty()
                ? compressedFile.getOriginalFilename()
                : "compressed.heif");

        File destOrigFile = new File(origDir, origUUID);
        File destCompFile = new File(compDir, compUUID);

        try {
            originalFile.transferTo(destOrigFile);
            if (compressedFile != null && !compressedFile.isEmpty()) {
                compressedFile.transferTo(destCompFile);
            }
        } catch (IOException e) {
            // 저장 도중 예외 발생 시 DB Insert 전체 트랜잭션 롤백
            throw new RuntimeException("로컬 파일 시스템에 이미지를 저장하는 중 오류가 발생했습니다.", e);
        }

        String origWebPath = "/images/original/" + datePath + "/" + origUUID;
        String compWebPath = "/images/compressed/" + datePath + "/" + compUUID;

        ImageMeta imageMeta = ImageMeta.builder()
                .groupId(groupId)
                .originalFilePath(origWebPath)
                .compressedFilePath(compWebPath)
                .latitude(latitude)
                .longitude(longitude)
                .altitude(altitude)
                .takenAt(takenAt != null ? takenAt : LocalDateTime.now())
                .build();

        imageMapper.insertImageMeta(imageMeta);
        return imageMeta.getImageId();
    }

    @Transactional
    public void endEventGroup(Long groupId) {
        imageMapper.updateGroupEnd(groupId);
    }
}