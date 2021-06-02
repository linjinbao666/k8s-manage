package ama.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

/**
 * 镜像vo类
 */

@EqualsAndHashCode
@Setter
@ToString
public class DockerImageVo {
    @JsonProperty("ID")
    private String imgID;
    @JsonProperty("Parent")
    private String parentId;
    @JsonProperty("Size")
    private Long size;
    @JsonProperty("Created")
    private String created;
    @JsonProperty("Build")
    private String build;
    @JsonProperty("Author")
    private String author;
    @JsonProperty("Dockerfile")
    private String dockerFile;

}
