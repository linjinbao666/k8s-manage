package ecs.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

@Data
@Setter
@Getter
@Entity
@ToString
@EntityListeners(AuditingEntityListener.class)
@ApiModel("存储卷申明")
@Table(name = "ecs_pvc")
public class Pvc implements Serializable {
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
    @Pattern(regexp = "^[a-z\\-0-9]+$", message = "只允许英文和数字和横杠")
    @ApiModelProperty(value = "存储卷申明名称", required = true)
    @Column(columnDefinition = "varchar(50) COMMENT '持久环卷索取名'")
    private String pvcName;

    @NotNull
    @Max(1000*10)
    @ApiModelProperty(value = "空间大小", required = true)
    @Column(columnDefinition = "int(11) COMMENT '空间大小/MB' ")
    private Long capaCity;

    @NotNull
    @ApiModelProperty(value = "访问模式", required = true, allowableValues = "ReadWriteMany, ReadOnlyMany, ReadWriteOnce")
    @Column(columnDefinition = "varchar(20) COMMENT '访问模式' ")
    private String accessModes;

    @ApiModelProperty(value = "服务器读取的创建时间", hidden = true)
    @Column(columnDefinition = "varchar(50) COMMENT '创建时间' ")
    private String created;

    @ApiModelProperty(value = "服务器读取的修改时间", hidden = true)
    @Column(columnDefinition = "varchar(50) COMMENT '创建时间' ")
    private String modified;

    @NotNull
    @ApiModelProperty(value = "名称空间", required = true)
    @Column(columnDefinition = "varchar(50) COMMENT '命名空间' ")
    private String namespace;

    @ApiModelProperty(value = "使用情况", hidden = true)
    @Column(columnDefinition = "varchar(20) COMMENT '使用情况' ")
    private String status;

    @ApiModelProperty(value = "存储卷名称", hidden = true)
    @Column(columnDefinition = "varchar(50) COMMENT 'pv名称' ")
    private String pvname;

    @NotNull
    @ApiModelProperty(value = "回收策略", required = true, allowableValues = "Recycle, Retain, Delete")
    @Column(columnDefinition = "varchar(50) COMMENT '回收策略' " )
    private String reclaimPolicy;
}
