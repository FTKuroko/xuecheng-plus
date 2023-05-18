package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.File;
import java.util.List;

/**
 * @description 媒资文件管理业务类
 * @author Mr.M
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService {

     /**
      * @description 媒资文件查询方法
      * @param pageParams 分页参数
      * @param queryMediaParamsDto 查询条件
      * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
      * @author Mr.M
      * @date 2022/9/10 8:57
     */
    public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    /**
     * @description 上传文件的通用接口
     * @param companyId 机构id
     * @param uploadFileParamsDto 文件信息
     * @param localFilePath 文件本地路径
     * @return UploadFileResultDto
     */
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath);
    /**
     * @description 将文件信息添加到文件表
     * @param companyId  机构id
     * @param fileMd5  文件md5值
     * @param uploadFileParamsDto  上传文件的信息
     * @param bucket  桶
     * @param objectName 对象名称
     * @return com.xuecheng.media.model.po.MediaFiles
     * @author Mr.M
     * @date 2022/10/12 21:22
     */
    public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName);

    /**
     * 检查文件是否存在
     * @param fileMd5 文件的 md5
     * @return
     */
    public RestResponse<Boolean> checkFile(String fileMd5);

    /**
     * 检查分块是否存在
     * @param fileMd5 文件的 md5
     * @param chunkIndex 分块序号
     * @return
     */
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

    /**
     * 上传分块
     * @param fileMd5  文件md5
     * @param chunk  分块序号
     * @param localChunkFilePath  分块文件本地路径
     * @return
     */
    public RestResponse uploadChunk(String fileMd5,int chunk,String localChunkFilePath);

    /**
     * 合并分块
     * @param companyId 机构 id
     * @param fileMd5 文件 md5
     * @param chunkTotal 分块总和
     * @param uploadFileParamsDto 文件信息
     * @return
     */
    public RestResponse mergechunks(Long companyId,String fileMd5,int chunkTotal,UploadFileParamsDto uploadFileParamsDto);
    /**
     * 从 minio 下载文件
     * @param bucket 桶
     * @param objectName 对象名称
     * @return
     */
    public File downloadFileFromMinIO(String bucket, String objectName);
    /**
     * 得到合并后的文件地址
     * @param fileMd5
     * @param fileExt
     * @return
     */
    public String getFilePathByMd5(String fileMd5,String fileExt);
    /**
     * 将文件上传到minio
     * @param localFilePath
     * @param mimeType
     * @param bucket
     * @param objectName
     * @return
     */
    public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName);

    /**
     * 根据媒资 id 查询媒资文件
     * @param mediaId
     * @return
     */
    MediaFiles getFileById(String mediaId);
}
