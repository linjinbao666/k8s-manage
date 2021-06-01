package ecs.entity;

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
import java.io.Serializable;
import java.util.Date;

/**
 * @author linjb
 * 资源管理-计算资源实体类
 */

@Data
@Setter
@Getter
@Entity
@ToString
@Table(name = "ecs_kubernetes_node")
@EntityListeners(AuditingEntityListener.class)
public class KubernetesNode implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;            //id
    @CreatedDate
    private Date createDate;    //创建日期
    @CreatedBy
    private String creator;     //创建者
    @LastModifiedDate
    private Date updateDate;    //修改者
    @LastModifiedBy
    private String updater;     //修改日期

    /**
     * 节点服务器IP
     */
    @Column(columnDefinition = "varchar(50) COMMENT '节点ip地址'")
    private String ip;

    /**
     * 主机名
     */
    @Column(columnDefinition = "varchar(50) COMMENT '主机名称'")
    private String hostname;

    /**
     * 节点cpu核数
     */
    @Column(columnDefinition = "int(11) COMMENT 'cpu个数/个'")
    private Integer cpu;

    /**
     * 节点内存
     */
    @Column(columnDefinition = "double(11,2) COMMENT '内存大小/GB'")
    private double memory;

    /**
     * 节点磁盘大小
     */
    @Column(columnDefinition = "double(11,2) COMMENT '磁盘大小/MB'")
    private double disk;

    /**
     * 节点标签
     */
    @Column(columnDefinition = "varchar(255) COMMENT '节点标签'")
    private String labels;

    /**
     * 节点状态
     */
    @Column(columnDefinition = "varchar(20) COMMENT '状态'")
    private String status;

}
