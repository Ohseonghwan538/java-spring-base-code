package com.map.egis.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.map.egis.config.StorageProperties;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageMapper imageMapper;
    private final StorageProperties storageProperties;

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1920;
    private static final float COMPRESSION_QUALITY = 0.80f;

    static {
        ImageIO.scanForPlugins();
    }

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
            MultipartFile originalFile,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal altitude,
            LocalDateTime takenAt
    ) {
        validateUpload(originalFile);

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path originalDirectory = storageProperties.originalDirectory().resolve(datePath);
        Path compressedDirectory = storageProperties.compressedDirectory().resolve(datePath);
        createDirectories(originalDirectory, compressedDirectory);

        String uuid = UUID.randomUUID().toString();
        String origFileName = uuid + "_" + sanitizeFilename(originalFile.getOriginalFilename());

        Path originalPath = originalDirectory.resolve(origFileName);

        LocalDateTime finalTakenAt;
        CompressedImage compressedImage;
        try {
            originalFile.transferTo(originalPath);
            finalTakenAt = resolveTakenAt(originalPath, takenAt);
            compressedImage = processAndSaveCompressedImage(originalPath, compressedDirectory, uuid);
        } catch (IOException | RuntimeException e) {
            deleteFilesQuietly(
                    originalPath,
                    compressedDirectory.resolve(uuid + ".webp"),
                    compressedDirectory.resolve(uuid + ".jpg")
            );
            throw new IllegalStateException("이미지 파일을 저장하거나 압축하는 중 오류가 발생했습니다.", e);
        }

        String origWebPath = "/images/original/" + datePath + "/" + origFileName;
        String compWebPath = "/images/compressed/" + datePath + "/" + compressedImage.fileName();

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

    private void validateUpload(MultipartFile originalFile) {
        if (originalFile == null || originalFile.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지 파일이 필요합니다.");
        }
    }

    private void createDirectories(Path... directories) {
        try {
            for (Path directory : directories) {
                Files.createDirectories(directory);
            }
        } catch (IOException e) {
            throw new IllegalStateException("이미지 저장 디렉터리를 만들 수 없습니다.", e);
        }
    }

    private String sanitizeFilename(String originalFilename) {
        String filename = originalFilename == null ? "image" : originalFilename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._-]", "_");
        return filename.isBlank() ? "image" : filename;
    }

    private void deleteFilesQuietly(Path... paths) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // The primary processing error is more useful to the caller than cleanup failure.
            }
        }
    }

    private LocalDateTime resolveTakenAt(Path imagePath, LocalDateTime requestTakenAt) {
        if (requestTakenAt != null) {
            return requestTakenAt;
        }
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imagePath.toFile());
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory != null) {
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }
        } catch (Exception ignored) {
            // Some supported image formats do not carry EXIF metadata.
        }
        
        return LocalDateTime.now();
    }

    private CompressedImage processAndSaveCompressedImage(Path sourcePath, Path destinationDirectory, String fileId)
            throws IOException {
        BufferedImage resizedImage = Thumbnails.of(sourcePath.toFile())
                .size(MAX_WIDTH, MAX_HEIGHT)
                .useExifOrientation(true)
                .asBufferedImage();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            writers = ImageIO.getImageWritersByFormatName("webp");
        }

        if (!writers.hasNext()) {
            Path jpegPath = destinationDirectory.resolve(fileId + ".jpg");
            if (!ImageIO.write(resizedImage, "jpg", jpegPath.toFile())) {
                throw new IOException("JPEG 이미지 인코더를 찾을 수 없습니다.");
            }
            return new CompressedImage(jpegPath.getFileName().toString());
        }

        Path webpPath = destinationDirectory.resolve(fileId + ".webp");
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(webpPath.toFile())) {
            if (ios == null) {
                throw new IOException("압축 이미지 출력 스트림을 열 수 없습니다.");
            }
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

        return new CompressedImage(webpPath.getFileName().toString());
    }

    private record CompressedImage(String fileName) {
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

    @Transactional(readOnly = true)
    public List<ImageMeta> findImages(String userId, Long groupId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("사용자 ID가 필요합니다.");
        }
        if (groupId == null) {
            throw new IllegalArgumentException("그룹 ID가 필요합니다.");
        }
        return imageMapper.findImagesByUserIdAndGroupId(userId.trim(), groupId);
    }
}
