package ama.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 镜像实体类
 * imageName = repository + tag
 */

@Entity
@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "dockerimage")
@EntityListeners(AuditingEntityListener.class)
public class DockerImage implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;                //id

    @CreatedDate
    private String createDate;    //创建日期

    @CreatedBy
    private String creator;       //创建者

    @LastModifiedDate
    private String updateDate;    //修改时间

    @ApiModelProperty(hidden = true, value = "仓库ID")
    @Column(columnDefinition = "bigint(20) default 0 COMMENT '仓库ID' ")
    private Long repositoryId;

    @Column(columnDefinition = "varchar(255) COMMENT '仓库名称' ")
    private String repository;          //例如busgbox

    @Column(columnDefinition = "varchar(255) COMMENT 'imgID' ")
    private String imgID;               //例如91e5fdfa7df3

    @Column(columnDefinition = "varchar(255) COMMENT '镜像别名=仓库名称:标签/版本' ")
    private String imgName;             //镜像名称

    @Column(columnDefinition = "varchar(50) COMMENT '镜像版本' ")
    private String imgVersion;          //版本

    @Column(columnDefinition = "bigint(20) COMMENT '镜像大小' " )
    private long size;                  //大小

    @Column(columnDefinition = "text COMMENT 'dockerfile内容md5值' ")
    private String dockerfileContent;

    @Column(columnDefinition = "int(11) COMMENT '镜像状态 0 数据库存在，服务器不存在， 1 都存在' ")
    private Integer status;

    /**用于标记远程仓库是否存在 0 不存在， 1 存在**/
    private Integer ifPublish; //0未发布 1 已经发布

}
