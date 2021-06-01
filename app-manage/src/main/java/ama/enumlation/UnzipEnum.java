package ama.enumlation;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 解压结果
 */


@Getter
@ToString
public enum UnzipEnum {
    OK,
    ERROR,
    UNZIP_FOLDER,
    DOCKERFILE;
}
