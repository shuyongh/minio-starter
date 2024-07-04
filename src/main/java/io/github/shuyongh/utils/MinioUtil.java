package io.github.shuyongh.utils;


import io.github.shuyongh.pojo.ObjectItem;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Administrator
 * @version 1.0
 * @description: TODO
 * @date 2024/7/4 0004 10:34
 */
@Component
@Slf4j
@Import({CommonUtil.class})
public class MinioUtil {
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private CommonUtil commonUtil;


    /**
     * 查看存储桶是否存在
     *
     * @param bucketName 存储桶名称
     * @return 桶是否存在
     */
    public boolean bucketExists(String bucketName) {
        boolean found;
        try {
            found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            found = false;
            e.printStackTrace();
        }
        return found;
    }
    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称
     * @return 是否创建成功
     */
    public Boolean makeBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    /**
     * 判断存储桶是否存在，不存在则创建
     *
     * @param bucketName 存储桶名称
     */
    public void isExistAndCreateBucket(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建了一个新桶"+bucketName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 删除存储桶
     *
     * @param bucketName 存储桶名称
     * @return 是否删除成功
     */
    public Boolean removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    /**
     * 判断对象是否存在
     *
     * @param bucketName 存储桶名称
     * @param objectName MinIO中存储对象全路径
     * @return 对象是否存在
     */
    public boolean existObject(String bucketName, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 上传多个文件
     *
     * @param bucketName 存储桶名称
     * @param fileList   文件列表
     * @return 桶中位置列表
     */
    public List<String> upload(String bucketName, List<MultipartFile> fileList) {
        MultipartFile[] fileArr = fileList.toArray(new MultipartFile[0]);
        return upload(bucketName, fileArr);
    }

    /**
     * 上传单个文件
     *
     * @param bucketName 存储桶名称
     * @param file       文件
     * @return 桶中位置
     */
    public String upload(String bucketName, MultipartFile file) {
        MultipartFile[] fileArr = {file};
        List<String> fileNames = upload(bucketName, fileArr);
        return fileNames.size() == 0 ? null : fileNames.get(0);
    }

    /**
     * 上传多个文件
     *
     * @param bucketName 存储桶名称
     * @param fileArr    文件列表
     * @return 桶中位置列表
     */
    public List<String> upload(String bucketName, MultipartFile[] fileArr) {
        // 保证桶一定存在
        isExistAndCreateBucket(bucketName);
        // 执行正常操作
        List<String> bucketFileNames = new ArrayList<>(fileArr.length);
        for (MultipartFile file : fileArr) {
            // 获取桶中文件名称
            String filename = file.getOriginalFilename();
            // 获取文件后缀
            String suffix = StringUtils.getFilenameExtension(filename);
            String bucketFileName = "";
            // 获取原始文件名称
            try (InputStream inputStream = file.getInputStream()) {

                filename = new  StringBuffer()
                        .append(DigestUtils.md5DigestAsHex(inputStream))
                        .append(".")
                        .append(suffix).toString();
                //构建上传路径
                 bucketFileName = commonUtil.buildFilePath(filename);
                // 推送文件到MinIO
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(bucketFileName)
                        .stream(inputStream, inputStream.available(), -1)
                        .contentType(file.getContentType())
                        .build()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            bucketFileNames.add(bucketFileName);
        }
        return bucketFileNames;
    }

    /**
     * 文件下载
     *
     * @param bucketName       存储桶名称
     * @param bucketFileName   桶中文件名称
     * @param originalFileName 原始文件名称
     * @param response         response对象
     */
    public void download(String bucketName, String bucketFileName, String originalFileName, HttpServletResponse response) {
        //封装文件下载参数
        GetObjectArgs objectArgs = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(bucketFileName)
                .build();
        //将文件读入到内存中，并进行一些处理，然后再将处理后的数据返回前端
        try (InputStream inputStream = minioClient.getObject(objectArgs);
             FastByteArrayOutputStream faos = new FastByteArrayOutputStream();
             ServletOutputStream stream = response.getOutputStream()
        ) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) != -1) {
                faos.write(buf, 0, len);
            }
            faos.flush();
            //对数据进行处理
            byte[] bytes = faos.toByteArray();
            response.setCharacterEncoding("utf-8");
            //设置强制下载不打开
            response.setContentType("application/force-download");
            // 设置附件名称编码
            originalFileName = new String(originalFileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            // 设置附件名称
            response.addHeader("Content-Disposition", "attachment;fileName=" + originalFileName);
            // 写入文件
            stream.write(bytes);
            stream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 获取已上传对象的文件流
     *
     * @param bucketName     存储桶名称
     * @param bucketFileName 桶中文件名称
     * @return 文件流
     */
    public InputStream getFileStream(String bucketName, String bucketFileName) throws Exception {
        GetObjectArgs objectArgs = GetObjectArgs.builder().bucket(bucketName).object(bucketFileName).build();
        return minioClient.getObject(objectArgs);
    }
    /**
     * 批量删除文件对象
     *
     * @param bucketName      存储桶名称
     * @param bucketFileNames 桶中文件名称集合
     * @return 删除结果迭代集合
     */
    private Iterable<Result<DeleteError>> removeObjects(String bucketName, List<String> bucketFileNames) {
        List<DeleteObject> dos = bucketFileNames.stream().map(DeleteObject::new).collect(Collectors.toList());
        return minioClient.removeObjects(RemoveObjectsArgs.builder().bucket(bucketName).objects(dos).build());
    }
    /**
     * 批量删除文件对象结果
     *
     * @param bucketName      存储桶名称
     * @param bucketFileNames 桶中文件名称集合
     * @return 删除结果
     */
    public List<DeleteError> removeObjectsResult(String bucketName, List<String> bucketFileNames) {
        Iterable<Result<DeleteError>> results = removeObjects(bucketName, bucketFileNames);
        List<DeleteError> res = new ArrayList<>();
        for (Result<DeleteError> result : results) {
            try {
                res.add(result.get());
            } catch (Exception e) {
                e.printStackTrace();
                log.error("遍历删除结果出现错误：" + e.getMessage());
            }
        }
        return res;
    }
    /**
     * 删除单个文件对象结果
     *
     * @param bucketName      存储桶名称
     * @param bucketFileName 桶中文件名称
     * @return 删除结果
     */
    public DeleteError removeObjectsResult(String bucketName, String bucketFileName) {
        List<DeleteError> results = removeObjectsResult(bucketName, Collections.singletonList(bucketFileName));
        return results.size() > 0 ? results.get(0) : null;
    }

    /**
     * 查看文件对象
     *
     * @param bucketName 存储桶名称
     * @return 文件对象集合
     */
    public List<ObjectItem> listObjects(@NonNull String bucketName) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).build());
        List<ObjectItem> objectItems = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                ObjectItem objectItem = new ObjectItem();
                objectItem.setObjectName(item.objectName());
                objectItem.setSize(item.size());
                objectItems.add(objectItem);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return objectItems;
    }
    /**
     * 获取桶（不限制桶类型）中临时文件访问url
     *
     * @param bucketName     存储桶名称
     * @param bucketFileName 桶中文件名称
     * @param expiry         过期时间数量
     * @param timeUnit       过期时间单位
     * @return 访问url
     */
    public String getUploadedObjectUrl(String bucketName, String bucketFileName, Integer expiry, TimeUnit timeUnit) {
        GetPresignedObjectUrlArgs urlArgs = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(bucketFileName)
                .expiry(expiry, timeUnit)
                .build();
        try {
            return minioClient.getPresignedObjectUrl(urlArgs);
        } catch (Exception e) {
            log.error("获取已上传文件的 Url 失败：" + e.getMessage());
            return "";
        }
    }
}
