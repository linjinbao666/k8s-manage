package ecs.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.stereotype.Service;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * @author linjb
 * 资源管理-存储资源实体类
 */

@Data
@Entity
@ToString
@Table(name = "ecs_pv")
@EntityListeners(AuditingEntityListener.class)
public class Pv implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;            //id
//    private String name;        //名称
//    private String code;        //代号
//    private long version;       //版本
//    private String nameLike;    //别名
    @CreatedDate
    private Date createDate;    //创建日期
    @CreatedBy
    private String creator;     //创建者
    @LastModifiedDate
    private Date updateDate;    //修改者
    private String updater;     //修改日期

    /**
     * 持久化卷名称
     */
    @Column(columnDefinition = "varchar(50) COMMENT '持久化卷名称' ")
    private String pvName;

    /**
     * 空间大小
     */
    @Column(columnDefinition = "int(11) COMMENT '空间大小/MB' ")
    private Integer capaCity;

    /**
     * 访问模式
     */
    @Column(columnDefinition = "varchar(20) COMMENT '访问模式' ")
    private String accessModes;

    /**
     * 回收策略
     */
    @Column(columnDefinition = "varchar(20) COMMENT '回收策略' ")
    private String reclaimPolicy;


    /**
     * 请求磁盘大小
     */
    @Column(columnDefinition = "int(11) COMMENT '请求磁盘大小' ")
    private Integer requestSystem;

    /**
     * 最大磁盘大小
     */
    @Column(columnDefinition = "int(11) COMMENT '最大磁盘大小' ")
    private Integer limitSystem;

    /**
     * 持久化卷的路径
     */
    @Column(columnDefinition = "varchar(50) COMMENT '持久化卷的路径' ")
    private String pvPath;

    /**
     * 创建时间（服务器pv创建时间）
     */
    @Column(columnDefinition = "datetime COMMENT '创建时间' ")
    private Date createTime;

    /**
     * 外部挂载路径
     */
    @Column(columnDefinition = "varchar(50) COMMENT '外部挂载路径' ")
    private String externalMountPath;

    /**
     * 持久化卷所绑定的持久化卷索取的名字
     */
    @Column(columnDefinition = "varchar(50) COMMENT '持久化卷所绑定的持久化卷索取的名字' ")
    private String pvcName;
    /**
     * 使用类型
     */
    @Column(columnDefinition = "int(11) COMMENT '使用类型' ")
    private short eventType;



}
