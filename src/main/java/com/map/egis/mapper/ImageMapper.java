package com.map.egis.mapper;

import com.map.egis.domain.ImageGroup;
import com.map.egis.domain.ImageMeta;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImageMapper {
    void insertImageGroup(ImageGroup group);
    void insertImageMeta(ImageMeta imageMeta);
    void updateGroupEnd(@Param("groupId") Long groupId);

    // 느낀점(memo) 업데이트 메서드 추가
    int updateImageMemo(@Param("imageId") Long imageId, @Param("memo") String memo);
    List<ImageMeta> findImagesByUserIdAndGroupId(
            @Param("userId") String userId,
            @Param("groupId") Long groupId
    );
}
