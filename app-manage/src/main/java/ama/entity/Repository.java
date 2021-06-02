package ama.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

@Entity
@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "repository")
@ApiModel(description = "镜像仓库实体类")
@EntityListeners(AuditingEntityListener.class)
public class Repository implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(hidden = true)
    private long id;

    @CreatedDate
    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "datetime COMMENT '创建日期'")
    private Date createDate;    //创建日期

    @CreatedBy
    @ApiModelProperty(hidden = true)
    @Column(columnDefinition="varchar(20) COMMENT '创建者'")
    private String creator;     //创建者

    @LastModifiedDate
    @ApiModelProperty(hidden = true)
    @Column(columnDefinition="datetime COMMENT '更新时间'")
    private Date updateDate;    //修改时间

    @NotNull
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "英文名称值允许大小写字母")
    @ApiModelProperty(value = "英文名称", required = true)
    @Column(columnDefinition = "varchar(50) COMMENT '仓库英文名称'")
    private String enName;

    @NotNull
    @Pattern(regexp = "[\\u4e00-\\u9fa5]+", message = "中文名称仅允许中文")
    @ApiModelProperty(value = "中文名称", required = true)
    @Column(columnDefinition = "varchar(50) COMMENT '仓库中文名称'")
    private String cnName;

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "int default 0 COMMENT '仓库镜像数量'")
    private Integer imgNum;

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "varchar(50)  COMMENT '最新版本'")
    private String latest;

    @ApiModelProperty(value = "组织id", hidden = true)
    @Column(columnDefinition = "bigint(20) COMMENT '组织id'")
    private long regId;

    @ApiModelProperty(value = "组织名称", hidden = true)
    @Column(columnDefinition = "varchar(150) COMMENT '组织名称'")
    private String regName;

    @ApiModelProperty(value = "仓库类型，public,  private", required = true, allowableValues = "public,private")
    @Column(columnDefinition = "varchar(15) COMMENT '仓库类型，public,private' ")
    private String type;

    @ApiModelProperty(value = "描述", required = true)
    @Column(columnDefinition = "text COMMENT '描述' ")
    private String description;
}
