package monitor.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
 * 黑白名单规则
 */

@ApiModel("黑白名单")
@Slf4j
@Data
@Getter
@Setter
@ToString
@Table(name = "IPRule")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class IPRule implements Serializable {

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
    @Length(min = 3, max = 15)
    @ApiModelProperty(value = "名称", required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = "应用名称", required = true, example = "vcode")
    private String appName;

    @NotNull
    @ApiModelProperty(allowableValues = "0,1", value = "黑白名单", required = true)
    private Integer type;   //类型 0 白名单 1 黑名单

    @NotNull
    @Length(min = 7)
    @ApiModelProperty(value = "具体规则", required = true, example = "*.16.12.0/16")
    private String rule;    //具体规则

    @ApiModelProperty(value = "状态", allowableValues = "0,1", notes = "0 禁用 1 启用")
    private Integer status; //状态

}
