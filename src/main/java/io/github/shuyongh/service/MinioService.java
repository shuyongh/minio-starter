package io.github.shuyongh.service;

import io.minio.messages.DeleteError;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

public interface MinioService {


    // 上传文件
    public String uploadFile(MultipartFile file);

    // 删除文件
    public DeleteError deleteFile(String bucketFileName);

    // 下载文件
    public void downloadFile(String bucketFileName,String originalFilename, HttpServletResponse response);

    // 获取文件临时分享地址
    public String shareUrl(String bucketFileName);


}
