package monitor.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 告警规则
 */

@ApiModel("告警管理实体类")
@Slf4j
@Data
@Getter
@Setter
@ToString
@Table(name = "alertRule")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class AlertRule implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(hidden = true)
    private long id;
    @CreatedDate
    @ApiModelProperty(hidden = true)
    private Date createDate;    //创建日期
    @CreatedBy
    @ApiModelProperty(hidden = true)
    private String creator;     //创建者
    @LastModifiedDate
    @ApiModelProperty(hidden = true)
    private Date updateDate;    //修改时间

    @NotNull
    @Length(min = 5, max = 15)
    @ApiModelProperty(required = true, value = "名称", example = "abcd")
    private String alertName;

    @ApiModelProperty(hidden = true, value = "英文名称， 用作alertRule文件名称")
    private String enName;

    @NotNull
    @ApiModelProperty(required = true, value = "应用名称", example = "vcode")
    private String appName;     //应用名称

    @NotNull
    @ApiModelProperty(required = true, value = "用户信息", example = "admin")
    private String user;        //用户

    @ApiModelProperty(value = "用户名称")
    private String userName;

    @NotNull
    @ApiModelProperty(required = true,value = "指标", example = "cpu",
            allowableValues = "cpu,memory,disk,appExist,diskIoRead,diskIoWrite,netIoRead,netIoWrite")
    private String target;      //指标

    @NotNull
    @ApiModelProperty(required = true, value = "阈值", example = "80")
    private String quota;       //阈值

    @ApiModelProperty(required = true, value = "邮件开关，默认开启", example = "1")
    private Integer mail;       //是否邮件告警 0 否 1 是

    @ApiModelProperty(required = true, value = "状态，默认开启", example = "1")
    private Integer status;     //状态 0 否 1 开启

    @ApiModelProperty(hidden = true, value = "告警描述")
    private String alertDesc;
}
