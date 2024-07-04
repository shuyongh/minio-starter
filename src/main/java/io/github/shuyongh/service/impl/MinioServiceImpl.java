package io.github.shuyongh.service.impl;



import io.github.shuyongh.config.MinioConfig;
import io.github.shuyongh.config.MinioPropertyConfig;
import io.github.shuyongh.service.MinioService;
import io.github.shuyongh.utils.MinioUtil;
import io.minio.messages.DeleteError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Slf4j
@EnableConfigurationProperties(MinioPropertyConfig.class)
@Import({MinioConfig.class, MinioUtil.class})
public class MinioServiceImpl implements MinioService {

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private MinioPropertyConfig minioPropertyConfig;


    // 上传文件
    public String uploadFile( MultipartFile file) {
        return minioUtil.upload(minioPropertyConfig.getBucket(), file);
    }

    // 删除文件
    public DeleteError deleteFile(String bucketFileName) {
        return minioUtil.removeObjectsResult(minioPropertyConfig.getBucket(), bucketFileName);
    }

    // 下载文件
    public void downloadFile( String bucketFileName,  String originalFilename, HttpServletResponse response) {
        minioUtil.download(minioPropertyConfig.getBucket(), bucketFileName, originalFilename, response);
    }

    // 获取文件临时分享地址
    public String shareUrl( String bucketFileName) {
        return minioUtil.getUploadedObjectUrl(minioPropertyConfig.getBucket(), bucketFileName, 7, TimeUnit.DAYS);
    }
}
