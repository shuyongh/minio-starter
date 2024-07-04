package io.github.shuyongh.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "syh.minio")
@Data
public class MinioPropertyConfig {

    // minio用户名
    private String accessKey;
    //minio密码
    private String secretKey;

    //minio的IP地址
    private String endpoint="http://127.0.0.1:9000";

    //根目录的名称
    private String bucket;

    //访问的路径
    private String baseUrl="http://127.0.0.1:9001";
}
