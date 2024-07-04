package io.github.shuyongh.config;


import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@EnableConfigurationProperties(MinioPropertyConfig.class)
@Configuration
public class MinioConfig {

    @Autowired
    private MinioPropertyConfig minioPropertyConfig;


    @Bean
    public MinioClient buildMinioClient(){
        return MinioClient
                .builder()
                .credentials(minioPropertyConfig.getAccessKey(), minioPropertyConfig.getSecretKey())
                .endpoint(minioPropertyConfig.getEndpoint())
                .build();
    }
}
