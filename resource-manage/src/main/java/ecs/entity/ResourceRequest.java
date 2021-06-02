package ecs.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

/**
 * @author linjb
 * @date 2020-08-14
 * 功能:厂商对k8s的资源申请实体类
 */

@DynamicUpdate
@DynamicInsert
@Data
@Setter
@Getter
@ToString
@Entity
@Table(name = "ecs_resource_request")
@ApiModel(description = "资源申请实体类")
@EntityListeners(AuditingEntityListener.class)
public class ResourceRequest implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(hidden = true)
    private long id;

    @CreatedDate
    @ApiModelProperty(hidden = true)
    private Date createDate;

    @CreatedBy
    @ApiModelProperty(hidden = true)
    private String creator;

    @LastModifiedDate
    @ApiModelProperty(hidden = true)
    private Date updateDate;

    @LastModifiedBy
    @ApiModelProperty(hidden = true)
    private String updater;

    @ApiModelProperty(value = "厂商名称")
    @Column(columnDefinition = "varchar(50) COMMENT '厂商名称'")
    private String companyName;

    @NotNull
    @ApiModelProperty(value = "CPU大小", required = true)
    @Column(columnDefinition = "int(11) COMMENT '厂商cpu总需求/核'")
    private Integer cpuRequests;


    @NotNull
    @ApiModelProperty(value = "内存大小", required = true)
    @Column(columnDefinition = "double(11,5) COMMENT '厂商内存总需求/GB'")
    private double memoryRequests;

    @NotNull
    @ApiModelProperty(value = "磁盘大小", required = true)
    @Column(columnDefinition = "double(11,5) COMMENT '厂商存储总需求/GB'")
    private double diskRequests;

    @NotNull
    @ApiModelProperty(value = "申请原因", required = true)
    @Column(columnDefinition = "varchar(100) COMMENT '申请原因'")
    private String reason;

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "bigint(20) COMMENT '用户id'")
    private Long userId;

    @ApiModelProperty(value = "组织ID", hidden = true)
    @Column(columnDefinition = "bigint(20) default 0 COMMENT '组织id'")
    private Long regId;

    @ApiModelProperty(value = "组织名称", hidden = true)
    @Column(columnDefinition = "varchar(50) COMMENT '组织名称'")
    private String regName;

    @ApiModelProperty(value = "分中心ID")
    @Column(columnDefinition = "bigint(20) default 0 COMMENT '分中心id'")
    private Long subId;

    @Pattern(regexp = "^[a-z0-9]+$", message = "名称空间允许小写字母和数字")
    @Column(columnDefinition = "varchar(50) COMMENT '命名空间'")
    @ApiModelProperty(value = "名称空间", required = true)
    private String namespace;

}
