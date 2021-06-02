package ama.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@ApiModel(description = "dockerfile实体类")
@Data
@Getter
@Setter
@Entity
@Table(name = "dockerfile")
@EntityListeners(AuditingEntityListener.class)
public class Dockerfile implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(hidden = true)
    private long id;            //id

    @ApiModelProperty(hidden = true)
    @CreatedDate
    private Date createDate;    //创建日期

    @ApiModelProperty(hidden = true)
    @CreatedBy
    private String creator;     //创建者

    @ApiModelProperty(hidden = true)
    @LastModifiedDate
    private Date updateDate;    //修改时间

    @ApiModelProperty(name = "fileName", value = "文件名称")
    @Column(columnDefinition = "varchar(20) COMMENT '文件名称'")
    private String fileName;    //文件名称

    @ApiModelProperty(name = "fileContent", value = "文件内容-base64处理的")
    @Column(columnDefinition = "text COMMENT 'dockerfile内容'")
    private String fileContent; //文件内容base64处理

}
