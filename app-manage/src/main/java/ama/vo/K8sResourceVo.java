package ama.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 资源vo类
 */
@Getter
@Setter
@ToString
public class K8sResourceVo {
    private String name;
    private long all;
    private long used;
//    private String format;
//    private String percentage;

}
