package io.github.shuyongh.utils;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Administrator
 * @version 1.0
 * @description: TODO
 * @date 2024/7/4 0004 11:06
 */
@Component
public class CommonUtil {
    /**
     * //按 /年/月/日/文件名 格式生成目录
     * @param filename  文件名
     * @return  文件路径
     */
    private final static String separator = "/";

    public String buildFilePath(@Nullable String filename) {

        String datePath = new SimpleDateFormat("yyyy/MM-dd/HH")
                .format(new Date());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(datePath).append(separator).append(filename);
        return stringBuffer.toString();
    }
}
