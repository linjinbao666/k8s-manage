package monitor.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
@ApiModel(value = "升级日志实体类")
public class AuditLogVo {
    @ApiModelProperty(value = "序号")
    private String id;

    @ApiModelProperty(value = "应用名称")
    private String appName;

    @ApiModelProperty(value = "容器名称")
    private String containerName;

    @ApiModelProperty(value = "请求IP")
    private String requestIP;

    @ApiModelProperty(value = "请求时间")
    private String requestTime;

    @ApiModelProperty(value = "请求耗时")
    private String ttl;

    @ApiModelProperty(value = "请求路劲")
    private String urlPath;

}
