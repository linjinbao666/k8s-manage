package ecs.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
 * 资源管理-服务器资源实体类
 */
@Data
@Setter
@Getter
@Entity
@ToString
@Table(name = "ecs_resource")
@ApiModel("服务器资源")
@EntityListeners(AuditingEntityListener.class)
public class EcsResource implements Serializable {
    private static final long serialVersionUID = 153627000034518368L;
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

    @NotNull
    @Pattern(regexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}")
    @ApiModelProperty(value = "ip地址", required = true)
    @Column(columnDefinition = "varchar(50) COMMENT 'ip地址'")
    private String ip;

    @ApiModelProperty(value = "主机名", hidden = true)
    @Column(columnDefinition = "varchar(50) COMMENT '主机名称'")
    private String hostname;

    @NotNull
    @ApiModelProperty(value = "远程端口",example = "22")
    @Column(columnDefinition = "int(11) COMMENT '远程端口'")
    private Integer remotePort;

    @ApiModelProperty(value = "状态", hidden = true)
    @Column(columnDefinition = "varchar(20) COMMENT '状态'")
    private String status;

    @NotNull(message = "用途不允许为空")
    @ApiModelProperty(value = "用途", required = true)
    @Column(columnDefinition = "varchar(50) default '通用' COMMENT '用途'")
    private String purpose;

    @ApiModelProperty(value = "操作系统")
    @Column(columnDefinition = "varchar(50) COMMENT '操作系统'")
    private String operatingSystem;

    @ApiModelProperty(value = "cpu个数", hidden = true)
    @Column(columnDefinition = "int(11) COMMENT 'cpu个数'")
    private Integer cpu;

    @ApiModelProperty(value = "内存大小", hidden = true)
    @Column(columnDefinition = "int(11) COMMENT '内存大小/GB'")
    private Integer memory;

    @ApiModelProperty(value = "硬盘格式", hidden = true)
    @Column(columnDefinition = "varchar(50) COMMENT '磁盘格式'")
    private String diskSystem;

    @ApiModelProperty(value = "硬盘大小", hidden = true)
    @Column(columnDefinition = "int(50) COMMENT '磁盘大小/GB'")
    private Integer diskData;

    @NotNull
    @ApiModelProperty(value = "root账号", required = true)
    @Column(columnDefinition = "varchar(50) COMMENT 'root账号'")
    private String account;

    @NotNull
    @ApiModelProperty(value = "密码", required = true)
    @Column(columnDefinition = "varchar(50) COMMENT 'root密码'")
    private String password;

    @ApiModelProperty(value = "映射IP")
    @Column(columnDefinition = "varchar(50) COMMENT '映射ip'")
    private String mappingIp;
}
