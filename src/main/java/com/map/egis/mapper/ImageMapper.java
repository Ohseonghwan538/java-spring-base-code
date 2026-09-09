package com.map.egis.mapper;

import com.map.egis.domain.ImageGroup;
import com.map.egis.domain.ImageMeta;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImageMapper {
    void insertImageGroup(ImageGroup group);
    void insertImageMeta(ImageMeta imageMeta);
    void updateGroupEnd(@Param("groupId") Long groupId);

    // 느낀점(memo) 업데이트 메서드 추가
    int updateImageMemo(@Param("imageId") Long imageId, @Param("memo") String memo);
    // 단건 조회 메서드 
    ImageMeta selectImageMetaById(Long imageId);
}
