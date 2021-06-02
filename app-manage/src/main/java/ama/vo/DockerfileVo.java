package ama.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Dockerfile vo类
 */

@Data
@Getter
@Setter
public class DockerfileVo {
    private String fileName;    //文件名称

    private String FROM;
    private String MAINTAINER;
    private String[] ADDS;
    private String[] ENVS;
    private String[] RUNS;
    private String[] EXPOSES;
    private String[] CMDS;

}
