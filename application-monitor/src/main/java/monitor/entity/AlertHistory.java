package monitor.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 告警历史
 */

@ApiModel("告警历史")
@Slf4j
@Data
@Getter
@Setter
@ToString
@Table(name = "alertHistory")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class AlertHistory implements Serializable {
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

    private Long ruleId;        //告警规则

    @ApiModelProperty(value = "告警容器")
    private String containerName;

    @ApiModelProperty(value = "告警指标")
    private String target;

    @ApiModelProperty(value = "告警值")
    private String quota;

    @ApiModelProperty(value = "邮件开关，默认开启")
    private Integer mail;


}
