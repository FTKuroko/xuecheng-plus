package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;
    @Autowired
    MinioClient minioClient;
    @Autowired
    MediaFileService mediaFileService;
    // 从配置文件中获取 bucket
    @Value("${minio.bucket.files}")
    private String bucket_files;
    @Value("${minio.bucket.videofiles}")
    private String bucket_video;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(!StringUtils.isEmpty(queryMediaParamsDto.getFilename()), MediaFiles::getFilename, queryMediaParamsDto.getFilename());
        queryWrapper.eq(!StringUtils.isEmpty(queryMediaParamsDto.getFileType()), MediaFiles::getFileType, queryMediaParamsDto.getFileType());
        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }
    //获取文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String folder = sdf.format(new Date()).replace("-", "/")+"/";
        return folder;
    }

    //获取文件的md5
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String fileMd5 = DigestUtils.md5DigestAsHex(fileInputStream);
            return fileMd5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    //根据扩展名获取mimeType
    private String getMimeType(String extension){
        if(extension == null){
            extension = "";
        }
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
        if(extensionMatch!=null){
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    /**
     * 将文件上传到minio
     * @param localFilePath
     * @param mimeType
     * @param bucket
     * @param objectName
     * @return
     */
    public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName){
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)//桶
                    .filename(localFilePath) //指定本地文件路径
                    .object(objectName)//对象名 放在子目录下
                    .contentType(mimeType)//设置媒体文件类型
                    .build();
            //上传文件
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("上传文件到minio成功,bucket:{},objectName:{},错误信息:{}",bucket,objectName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件出错,bucket:{},objectName:{},错误信息:{}",bucket,objectName,e.getMessage());
        }
        return false;
    }

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
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName){
        //将文件信息保存到数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if(mediaFiles == null){
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto,mediaFiles);
            //文件id
            mediaFiles.setId(fileMd5);
            //机构id
            mediaFiles.setCompanyId(companyId);
            //桶
            mediaFiles.setBucket(bucket);
            //file_path
            mediaFiles.setFilePath(objectName);
            //file_id
            mediaFiles.setFileId(fileMd5);
            //url
            mediaFiles.setUrl("/"+bucket+"/"+objectName);
            //上传时间
            mediaFiles.setCreateDate(LocalDateTime.now());
            //状态
            mediaFiles.setStatus("1");
            //审核状态
            mediaFiles.setAuditStatus("002003");
            //插入数据库
            int insert = mediaFilesMapper.insert(mediaFiles);
            if(insert<=0){
                log.debug("向数据库保存文件失败,bucket:{},objectName:{}",bucket,objectName);
                return null;
            }
            return mediaFiles;

        }
        return mediaFiles;

    }

    /**
     * 上传图片文件
     * @param companyId 机构id
     * @param uploadFileParamsDto 文件信息
     * @param localFilePath 文件本地路径
     * @return
     */
    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {

        //文件名
        String filename = uploadFileParamsDto.getFilename();
        //先得到扩展名
        String extension = filename.substring(filename.lastIndexOf("."));

        //得到mimeType
        String mimeType = getMimeType(extension);

        //子目录
        String defaultFolderPath = getDefaultFolderPath();
        //文件的md5值
        String fileMd5 = getFileMd5(new File(localFilePath));
        String objectName = defaultFolderPath+fileMd5+extension;
        //上传文件到minio
        boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucket_files, objectName);
        if(!result){
            XueChengPlusException.cast("上传文件失败");
        }
        //入库文件信息
        MediaFiles mediaFiles = mediaFileService.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_files, objectName);
        if(mediaFiles==null){
            XueChengPlusException.cast("文件上传后保存信息失败");
        }
        //准备返回的对象
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);

        return uploadFileResultDto;
    }

    /**
     * 检查文件是否存在
     * @param fileMd5 文件的md5
     * @return
     */
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //先查询数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if(mediaFiles!=null){
            //桶
            String bucket = mediaFiles.getBucket();
            //objectname
            String filePath = mediaFiles.getFilePath();
            //如果数据库存在再查询 minio
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filePath)
                    .build();
            //查询远程服务获取到一个流对象
            try {
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                if(inputStream!=null){
                    //文件已存在
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        //文件不存在
        return RestResponse.success(false);
    }

    /**
     * 检查分块是否存在
     * @param fileMd5  文件的md5
     * @param chunkIndex  分块序号
     * @return
     */
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

        //根据md5得到分块文件所在目录的路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);

        //如果数据库存在再查询 minio
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket_video)
                .object(chunkFileFolderPath+chunkIndex)
                .build();
        //查询远程服务获取到一个流对象
        try {
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            if(inputStream!=null){
                //文件已存在
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //文件不存在
        return RestResponse.success(false);
    }

    private String getChunkFileFolderPath(String fileMd5){
        // 文件存储路径命名格式:fieldMd5第一位/filedMd2第二位/filedMd5
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    /**
     * 上传分块
     * @param fileMd5  文件md5
     * @param chunk  分块序号
     * @param localChunkFilePath  分块文件本地路径
     * @return
     */
    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
        //分块文件的路径
        String chunkFilePath = getChunkFileFolderPath(fileMd5) + chunk;
        //获取mimeType
        String mimeType = getMimeType(null);
        //将分块文件上传到minio
        boolean b = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_video, chunkFilePath);
        if(!b){
            return RestResponse.validfail(false,"上传分块文件失败");
        }
        //上传成功
        return RestResponse.success(true);
    }

    /**
     * 合并分块
     * @param companyId 机构 id
     * @param fileMd5 文件 md5
     * @param chunkTotal 分块总和
     * @param uploadFileParamsDto 文件信息
     * @return
     */
    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //分块文件所在目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //找到所有的分块文件
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(chunkTotal).map(i -> ComposeSource.builder().bucket(bucket_video).object(chunkFileFolderPath + i).build()).collect(Collectors.toList());
        //源文件名称
        String filename = uploadFileParamsDto.getFilename();
        //扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //合并后文件的objectname
        String objectName = getFilePathByMd5(fileMd5, extension);
        //指定合并后的objectName等信息
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucket_video)
                .object(objectName)//合并后的文件的objectname
                .sources(sources)//指定源文件
                .build();
        //===========合并文件============
        //报错size 1048576 must be greater than 5242880，minio默认的分块文件大小为5M
        try {
            minioClient.composeObject(composeObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错,bucket:{},objectName:{},错误信息:{}",bucket_video,objectName,e.getMessage());
            return RestResponse.validfail(false,"合并文件异常");
        }

        //===========校验合并后的和源文件是否一致，视频上传才成功===========
        //先下载合并后的文件
        File file = downloadFileFromMinIO(bucket_video, objectName);
        try(FileInputStream fileInputStream = new FileInputStream(file)){
            //计算合并后文件的md5
            String mergeFile_md5 = DigestUtils.md5DigestAsHex(fileInputStream);
            //比较原始md5和合并后文件的md5
            if(!fileMd5.equals(mergeFile_md5)){
                log.error("校验合并文件md5值不一致,原始文件:{},合并文件:{}",fileMd5,mergeFile_md5);
                return RestResponse.validfail(false,"文件校验失败");
            }
            //文件大小
            uploadFileParamsDto.setFileSize(file.length());
        }catch (Exception e) {
            return RestResponse.validfail(false,"文件校验失败");
        }

        //==============将文件信息入库============
        MediaFiles mediaFiles = mediaFileService.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_video, objectName);
        if(mediaFiles == null){
            return RestResponse.validfail(false,"文件入库失败");
        }
        //==========清理分块文件=========
        clearChunkFiles(chunkFileFolderPath,chunkTotal);

        return RestResponse.success(true);
    }

    /**
     * 从 minio 下载文件
     * @param bucket 桶
     * @param objectName 对象名称
     * @return
     */
    public File downloadFileFromMinIO(String bucket,String objectName){
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try{
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile=File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream,outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(outputStream!=null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 得到合并后的文件地址
     * @param fileMd5
     * @param fileExt
     * @return
     */
    private String getFilePathByMd5(String fileMd5,String fileExt){
        return   fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }

    /**
     * 清除分块文件
     * @param chunkFileFolderPath 分块文件路径地址
     * @param chunkTotal 分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath,int chunkTotal){
        Iterable<DeleteObject> objects =  Stream.iterate(0, i -> ++i).limit(chunkTotal).map(i -> new DeleteObject(chunkFileFolderPath+ i)).collect(Collectors.toList());;
        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucket_video).objects(objects).build();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
        //要想真正删除
        results.forEach(f->{
            try {
                DeleteError deleteError = f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

}