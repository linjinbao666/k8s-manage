package ama.vo;

import lombok.Data;
import lombok.Setter;

import java.io.Serializable;

/**
 * 应用pod
 */

@Setter
@Data
public class AppPodVo implements Serializable {

    /**
     * pod
     */
    private String podName;

    /**
     * 容器端口号
     */
    private Integer port;

    /**
     * 容器状态
     */
    private String status;

    /**
     * 容器IP
     */
    private String ip;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 重启次数
     */
    private Integer restartCount;

    /**
     * 创建时间
     */
    private String createdTime;

}
