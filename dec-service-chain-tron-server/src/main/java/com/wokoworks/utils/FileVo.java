package com.wokoworks.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 飞
 * @Date: 2021/12/14 16:50
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileVo {

    private FileType fileType;
    private String fileUrl;
    private String smallImage = "";
    private String name = "";
}
