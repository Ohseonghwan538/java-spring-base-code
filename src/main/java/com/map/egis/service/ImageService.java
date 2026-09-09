package com.map.egis.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.map.egis.domain.ImageGroup;
import com.map.egis.domain.ImageMeta;
import com.map.egis.mapper.ImageMapper;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageMapper imageMapper;

    private static final int MAX_WIDTH = 1920; // 지도 요구사항에 맞춰 640/1280/1920으로 조절
    private static final int MAX_HEIGHT = 1920; // // 지도 요구사항에 맞춰 480/720/1080으로 조절
    private static final float COMPRESSION_QUALITY = 0.80f;

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
    public ImageMeta saveImage(
            Long groupId,
            MultipartFile originalFile, // compressedFile 파라미터 제거 (서버 자동 생성)
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal altitude,
            LocalDateTime takenAt
    ) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String projectRoot = new File("").getAbsolutePath();

        String origDir = projectRoot + "/storage/original/" + datePath;
        String compDir = projectRoot + "/storage/compressed/" + datePath;

        new File(origDir).mkdirs();
        new File(compDir).mkdirs();

        String uuid = UUID.randomUUID().toString();
        String origFileName = uuid + "_" + originalFile.getOriginalFilename();
        String compFileName = uuid + ".webp";

        File destOrigFile = new File(origDir, origFileName);
        File destCompFile = new File(compDir, compFileName);

        // 1. 원본 저장
        try {
            originalFile.transferTo(destOrigFile);
        } catch (IOException e) {
            throw new RuntimeException("로컬 파일 시스템에 원본 이미지를 저장하는 중 오류가 발생했습니다.", e);
        }

        // 2. 촬영 시각 결정 (전달값 -> EXIF 추출 -> 현재시각)
        LocalDateTime finalTakenAt = resolveTakenAt(destOrigFile, takenAt);

        // 3. 메타데이터 제거 및 압축 이미지 생성
        processAndSaveCompressedImage(destOrigFile, destCompFile);

        String origWebPath = "/images/original/" + datePath + "/" + origFileName;
        String compWebPath = "/images/compressed/" + datePath + "/" + compFileName;

        ImageMeta imageMeta = ImageMeta.builder()
                .groupId(groupId)
                .originalFilePath(origWebPath)
                .compressedFilePath(compWebPath)
                .latitude(latitude)
                .longitude(longitude)
                .altitude(altitude)
                .takenAt(finalTakenAt)
                .build();

        imageMapper.insertImageMeta(imageMeta);
        return imageMeta;
    }

    private LocalDateTime resolveTakenAt(File imageFile, LocalDateTime requestTakenAt) {
        if (requestTakenAt != null) {
            return requestTakenAt;
        }
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory != null) {
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }
        } catch (Exception ignored) {}
        
        return LocalDateTime.now();
    }

    private void processAndSaveCompressedImage(File srcFile, File destFile) {
        // TwelveMonkeys WebP 플러그인 동적 레지스트리 등록
        ImageIO.scanForPlugins();
        try {
            // 1. 원본 비율 유지 리사이징 및 EXIF 회전 적용
            BufferedImage resizedImage = Thumbnails.of(srcFile)
                    .size(MAX_WIDTH, MAX_HEIGHT)
                    .useExifOrientation(true)
                    .asBufferedImage();

            // 2. WebP 인코더 검색 (MIME type 또는 Format name)
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
            if (!writers.hasNext()) {
                writers = ImageIO.getImageWritersByFormatName("webp");
            }

            // WebP 지원 불가 시 JPG로 Fallback 처리
            if (!writers.hasNext()) {
                boolean written = ImageIO.write(resizedImage, "jpg", destFile);
                if (!written) {
                    throw new RuntimeException("ImageIO를 통한 이미지 저장 실패");
                }
                return;
            }

            // 3. WebP 인코딩 진행
            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(destFile)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();

                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    String[] types = param.getCompressionTypes();
                    if (types != null && types.length > 0) {
                        param.setCompressionType(types[0]);
                    }
                    param.setCompressionQuality(COMPRESSION_QUALITY);
                }

                writer.write(null, new IIOImage(resizedImage, null, null), param);
            } finally {
                writer.dispose();
            }

        } catch (IOException e) {
            throw new RuntimeException("이미지 압축 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public void endEventGroup(Long groupId) {
        imageMapper.updateGroupEnd(groupId);
    }

    // 이미지 느낀점(memo) 저장 및 수정
    @Transactional
    public void updateImageMemo(Long imageId, String memo) {
        int updatedRows = imageMapper.updateImageMemo(imageId, memo);
        if (updatedRows == 0) {
            throw new IllegalArgumentException("해당 이미지 메타 정보가 존재하지 않습니다. ID: " + imageId);
        }
    }
}