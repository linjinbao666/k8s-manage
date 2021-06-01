package ecs.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 分中心
 */

@ApiModel("分中心实体类")
@Data
@Setter
@Getter
@ToString
@Entity
@Table(name = "ecs_subCenter")
@EntityListeners(AuditingEntityListener.class)
public class SubCenter implements Serializable {

    @ApiModelProperty(hidden = true,value = "记录id，用作后续的subId")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ApiModelProperty(hidden = true)
    @CreatedDate
    private Date createDate;

    @ApiModelProperty(hidden = true)
    @CreatedBy
    private String creator;

    @ApiModelProperty(hidden = true)
    @LastModifiedDate
    private Date updateDate;

    @NotNull
    @Length(min = 3, max = 20)
    @ApiModelProperty(required = true, value = "分中心名称")
    @Column(columnDefinition = "varchar(50) COMMENT '分中心名称'")
    private String centerName;

    @Min(value = 1, message = "最小为1核")
    @Max(value = 20, message = "最大为20核")
    @ApiModelProperty(required = true, value = "分中心cpu总需求")
    @Column(columnDefinition = "int(11) COMMENT '分中心cpu总需求'")
    private Integer cpuRequest;

    @DecimalMin(value = "1", message = "最小1GB，请注意单位")
    @DecimalMax(value = "1000000", message = "非法输入")
    @ApiModelProperty(required = true, value = "分中心内存总需求/MB")
    @Column(columnDefinition = "double(11,5) COMMENT '分中心内存总需求/MB'")
    private double memoryRequest;

    @DecimalMin(value = "1", message = "最小1GB，请注意单位")
    @DecimalMax(value = "100000000", message = "非法输入")
    @ApiModelProperty(required = true, value = "分中心存储总需求/MB")
    @Column(columnDefinition = "double(11,5) COMMENT '分中心存储总需求/MB'")
    private double diskRequest;

    @ApiModelProperty(hidden = true,value = "cpu已使用")
    private double cpuUsed;

    @ApiModelProperty(hidden = true, value = "内存已使用")
    private double memoryUsed;

    @ApiModelProperty(hidden = true, value = "磁盘已使用")
    private double diskUsed;

}
