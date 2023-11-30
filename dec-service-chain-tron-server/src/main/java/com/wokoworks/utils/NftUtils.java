package com.wokoworks.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 飞
 * @Date: 2021/12/8 10:42
 */
@Slf4j
public class NftUtils {


    // private static final Cache<String, NftInfo> cache = CacheBuilder.newBuilder().maximumSize(100).expireAfterAccess(10, TimeUnit.MINUTES).build();

    //image, mp4, data:application/json;base64,
    public static NftInfo parseNft(String tokenUri) {
        tokenUri = getFilledTokenUri(tokenUri);
        log.debug("<===解析url:{}", tokenUri);

        if (Strings.isNullOrEmpty(tokenUri)) {
            return null;
        }

        /**
         * 直接判断出文件类型
         */
        FileType fileType = getFileType(tokenUri);
        if (fileType != null) {
            if (fileType == FileType.BASE64) {
                FileVo fileVo = parseBase64(tokenUri);
                NftInfo nftInfo = new NftInfo();
                nftInfo.setUri(tokenUri);
                nftInfo.setUriContent(tokenUri);
                nftInfo.setUriFileType(fileType);
                nftInfo.setInnerFile(fileVo.getFileUrl());
                nftInfo.setInnerFileType(fileVo.getFileType());
                nftInfo.setSmallImage(StringUtils.hasLength(fileVo.getSmallImage()) ? fileVo.getSmallImage() : fileVo.getFileUrl());
                return nftInfo;
            }

            if (fileType != FileType.JSON) {
                NftInfo nftInfo = new NftInfo();
                nftInfo.setUri(tokenUri);
                nftInfo.setUriContent(tokenUri);
                nftInfo.setUriFileType(fileType);
                nftInfo.setInnerFile(tokenUri);
                nftInfo.setInnerFileType(fileType);
                nftInfo.setSmallImage("");
                return nftInfo;
            }
        }

        /**
         * todo::缓存测试环境
         */
//        NftInfo nftInfo_cache = cache.getIfPresent(tokenUri);
//        if (nftInfo_cache != null) {
//            return nftInfo_cache;
//        }


        /**
         * 需要通过查询url才能判断出文件类型
         */
        HttpUtils.FileVo httpFileVo = HttpUtils.get(tokenUri);
        if (httpFileVo == null) {
            return null;
        }


        /**
         * 解析得到jsonurl
         */
        if (!Strings.isNullOrEmpty(httpFileVo.getContent()) && httpFileVo.getContent().trim().startsWith("{")) {
            FileVo file = parseJsonFile(httpFileVo.getContent());
            if (file == null) {
                log.info("<==httpFileVo{}", httpFileVo);
                return null;
            }
            NftInfo nftInfo = new NftInfo();
            nftInfo.setUri(tokenUri);
            nftInfo.setUriContent(httpFileVo.getContent());
            nftInfo.setUriFileType(FileType.JSON);
            nftInfo.setInnerFile(file.getFileUrl());
            nftInfo.setInnerFileType(file.getFileType());
            nftInfo.setSmallImage(StringUtils.hasLength(file.getSmallImage()) ? file.getSmallImage() : file.getFileUrl());
            nftInfo.setName(file.getName());
            //  cache.put(tokenUri, nftInfo);
            return nftInfo;
        }


        /**
         * 通过content type判断出文件类型(默认图片)
         */
        NftInfo nftInfo = new NftInfo();
        nftInfo.setUri(tokenUri);
        nftInfo.setUriContent(tokenUri);
        nftInfo.setInnerFile(tokenUri);
        nftInfo.setInnerFileType(FileType.IMAGE);
        nftInfo.setUriFileType(FileType.IMAGE);
        nftInfo.setSmallImage(tokenUri);

        String contentType = httpFileVo.getContentType();
        if (!Strings.isNullOrEmpty(contentType)) {
            if (contentType.contains("video")) {
                nftInfo.setUriFileType(FileType.VIDEO);
                nftInfo.setInnerFileType(FileType.VIDEO);
            }
            if (contentType.contains("mp3")) {
                nftInfo.setUriFileType(FileType.MP3);
                nftInfo.setInnerFileType(FileType.MP3);
            }

            if (contentType.contains("gif")) {
                nftInfo.setUriFileType(FileType.JIF);
                nftInfo.setInnerFileType(FileType.JIF);
            }
        }

        //   cache.put(tokenUri, nftInfo);
        return nftInfo;
    }


    public static FileVo parseJsonFile(String urlFileVo) {
        if (Strings.isNullOrEmpty(urlFileVo) || !urlFileVo.startsWith("{")) {
            return null;
        }

        JSONObject jsonObject = JSON.parseObject(urlFileVo);
        String file = null;
        String smallImage = "";
        String name = "";
        name = jsonObject.getString("name");

        if (file == null) {
            JSONObject media = jsonObject.getJSONObject("media");
            if (media != null) {
                file = media.getString("uri");
            }
        }

        if (file == null) {
            file = jsonObject.getString("animation_url");
        }

        if (file == null) {
            file = jsonObject.getString("image");
        }

        if (file == null) {
            file = jsonObject.getString("image_url");
        }

        if (file == null) {
            JSONObject propertiesObject = jsonObject.getJSONObject("properties");
            if (propertiesObject != null) {
                file = propertiesObject.getJSONObject("image").getString("description");
            }
            if (Strings.isNullOrEmpty(name)) {
                name = propertiesObject.getJSONObject("name").getString("description");
            }
        }

        if (file == null) {
            return null;
        }
        file = getFilledTokenUri(file);


        FileType fileType = getFileType(file);
        if (fileType == null) {
            fileType = FileType.IMAGE;
        }

        return FileVo.builder().fileUrl(file).fileType(fileType).name(name).smallImage(smallImage).build();
    }

    private static final Map<String, FileType> FILE_TYPE_MAP = new HashMap<>();

    static {
        FILE_TYPE_MAP.put("video", FileType.VIDEO);
        FILE_TYPE_MAP.put("mp4", FileType.VIDEO);
        FILE_TYPE_MAP.put("mp3", FileType.MP3);
        FILE_TYPE_MAP.put("gif", FileType.JIF);
        FILE_TYPE_MAP.put("mov", FileType.VIDEO);
        FILE_TYPE_MAP.put("jpeg", FileType.IMAGE);
        FILE_TYPE_MAP.put("jpg", FileType.IMAGE);
        FILE_TYPE_MAP.put("png", FileType.IMAGE);
    }

    private static FileType getFileType(String file) {
        FileType fileType = null;

        if (file.startsWith("data:application/json;base64,")) {
            return FileType.BASE64;
        }

        int index = file.lastIndexOf(".");
        if (index > 0) {
            String substring = file.substring(index + 1);
            fileType = FILE_TYPE_MAP.get(substring.toLowerCase());
        }

        if (fileType == null) {
            if (file.startsWith("data:image")) {
                fileType = FileType.DATA_IMAGE;
            }
        }
        return fileType;
    }


    public static FileVo parseBase64(String str) {
        if (str == null || str.length() <= 29) {
            return null;
        }
        String substring = str.substring(29);
        String jsonStr = decodeBySunMisc(substring);
        return parseJsonFile(jsonStr);
    }


    /**
     * sun.misc方式Base64解码
     *
     * @param str
     * @return
     */
    public static String decodeBySunMisc(String str) {
        return new String(Base64.getDecoder().decode(str));
    }


    public static String getFilledTokenUri(String tokenUri) {
        if (tokenUri == null || tokenUri.isEmpty()) {
            return "";
        }
        tokenUri = tokenUri.trim();

        // base64
        if (tokenUri.startsWith("data:")) {
            return tokenUri;
        }

        // 存在协议, 并且是ipfs
        if (tokenUri.startsWith("ipfs://")) {
            String base = "https://ipfs.io/";
            if (!tokenUri.startsWith("ipfs://ipfs")) {
                base += "ipfs/";
            }
            return base + tokenUri.substring(7);
        }

        if (tokenUri.startsWith("https://mintearte.tk")) {
            tokenUri = tokenUri + "/index.json";
        }

        if (tokenUri.startsWith("ipfs://")) {
            tokenUri = tokenUri.replace("ipfs://", "https://ipfs.io/ipfs/");
        }


        // 如果不存在协议,则认为是ipfs
        if (!tokenUri.contains("://")) {
            String base = "https://ipfs.io/";
            if (!tokenUri.startsWith("ipfs")) {
                base += "ipfs/";
            }
            return base + tokenUri;
        }


        return tokenUri;
    }


    @Data
    public static class NftInfo {
        private String uri;
        private String uriContent;
        private FileType uriFileType;
        private String innerFile;
        private FileType innerFileType;
        private String smallImage;

        // 由于ERC-1155 具体NFT名称需要进入json获取, 需要字段进行存储, 并预先赋值防止获取不到的情况
        private String name = "NFT";
    }


}
