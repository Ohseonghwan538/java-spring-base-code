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
}
