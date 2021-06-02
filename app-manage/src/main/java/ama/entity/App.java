package ama.entity;


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

@Data
@Getter
@Setter
@Entity
@Table(name = "app")
@ToString
@EntityListeners(AuditingEntityListener.class)
public class App implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(hidden = true)
    private long id;            //id

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

    @Column(columnDefinition = "varchar(50) COMMENT '中文名称'")
    @ApiModelProperty(value = "中文名称")
    private String cnName;

    @NotNull
    @Length(min = 3, max = 40, message="应用名称长度必须在3-40之间")
    @ApiModelProperty(value = "应用名称", required = true)
    @Column(columnDefinition = "varchar(50) COMMENT '应用名称'")
    private String appName;

    @ApiModelProperty(value = "名称空间")
    @Column(columnDefinition = "varchar(20) COMMENT '名称空间'")
    private String namespace;

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "varchar(20) COMMENT '版本'")
    private String version;

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "varchar(50) COMMENT '访问地址'")
    private String url;

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "varchar(20) COMMENT '状态'")
    private String status;      //状态

    /**
     *  deployment属性
     */

    @Min(1)
    @Max(10)
    @ApiModelProperty(value = "副本个数", required = true)
    @Column(columnDefinition = "int(11) COMMENT '副本个数'")
    private Integer replicas;       //副本个数

    @ApiModelProperty(value = "升级策略", required = true, allowableValues = "RollingUpdate,Recreate")
    @Column(columnDefinition = "varchar(20) COMMENT '升级策略'")
    private String updatePolicy;            //升级策略

    /**
     * template属性
     */
    @ApiModelProperty(value = "镜像名称", required = true)
    @Column(columnDefinition = "varchar(100) COMMENT '镜像名称'")
    private String imageName;       //镜像名称

    @ApiModelProperty(value = "镜像拉取策略", required = true, allowableValues = "IfNotPresent,Always,Never")
    @Column(columnDefinition = "varchar(20) COMMENT '拉取策略'")
    private String imagePolicy;     //镜像拉取策略

    @ApiModelProperty(value = "容器端口", hidden = true)
    @Column(columnDefinition = "int(11) COMMENT '容器端口，一般由制作镜像决定，例如nginx是80，tomcat是8080'")
    private Integer containerPort;   //容器端口

    @Length(max = 5)
    @Column(columnDefinition = "varchar(11) COMMENT '请求cpu大小，单位m'")
    @ApiModelProperty(value = "请求cpu大小/m", required = true)
    private String cpuAmount;      //请求cpu大小

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "varchar(10) COMMENT 'cpu单位'")
    private String cpuFormat="m";      //请求cpu单位

    @Length(max = 5)
    @ApiModelProperty(value = "请求内存大小", required = true)
    @Column(columnDefinition = "varchar(11) COMMENT '请求内存大小，单位Mi'")
    private String memoryAmount;   //请求内存大小

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "varchar(10) COMMENT '内存单位'")
    private String memoryFormat="Mi";   //请求内存单位

    @ApiModelProperty(value = "节点选择器")
    @Column(columnDefinition = "varchar(150) COMMENT '节点选择器'")
    private String nodeSelector;   //节点选择器

    @ApiModelProperty(value = "存储卷名称,存储卷个数需要和存储路径对应，例如挂载存储卷pva, 挂载路径为/home；挂载存储卷pvb，挂载路径/opt")
    @Column(columnDefinition = "varchar(150) COMMENT '存储卷名称'")
    private String pvcNames;       //存储卷名称,分割

    @ApiModelProperty(value = "挂载路径和存储卷需要配合")
    @Column(columnDefinition = "varchar(150) COMMENT '挂载路径'")
    private String mountPaths;     //挂载路径,分隔

    /**
     * service属性
     */

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "varchar(20) COMMENT '集群内部ip'")
    private String clusterIP;       //ip

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "varchar(50) COMMENT '端口暴露策略'")
    private String externalTrafficPolicy;   //暴露策略

    @ApiModelProperty(value = "端口, 只有在serviceType为nodePort的时候生效")
    @Column(columnDefinition = "int(11) COMMENT '外部暴露端口'")
    private Integer nodePort;        //nodePort

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "varchar(11) COMMENT '目标端口，一般和上面的容器端口一致，可能非数字类型，例如web'")
    private String targetPort;      //targetPort

    @ApiModelProperty(value = "环境变量,以分号隔开多组，以逗号隔开key和value，前面是base64转码后的key，后面是base64转码后的value", example = "TVlTUUxfUk9PVF9QQVNTV09SRA==,MzY5MzY5;TVlTUUxfUk9PVF9QQVNTV09SRA==,MzY5MzY5")
    @Column(columnDefinition = "varchar(100) COMMENT '环境变量'")
    private String envs;            //环境变量格式：TVlTUUxfUk9PVF9QQVNTV09SRA==,MzY5MzY5;TVlTUUxfUk9PVF9QQVNTV09SRA==,MzY5MzY5

    @ApiModelProperty(value = "域名,域名,以分号隔开多组，以逗号隔开ip和域名：*.15.64.2,google.com,baidu.com;*.232.232.2,a.b.com")
    @Column(columnDefinition = "varchar(150) COMMENT '域名'")
    private String dominName;        //域名格式：*.15.64.2,google.com,baidu.com;*.232.232.2,a.b.com

    @ApiModelProperty(value = "探针,用于定时检测程序运行状态", example = "/healthz")
    @Column(columnDefinition = "varchar(150) COMMENT '探针，此处只支持httpGet，对应文件中的path'")
    private String pointer;         //探针

    @ApiModelProperty(value = "端口暴露类型，可选", allowableValues = "ClusterIP,NodePort,INGRESS")
    @Column(columnDefinition = "varchar(20) COMMENT '端口暴露类型， ClusterIP,NodePort,INGRESS'")
    private String serviceType;

    @ApiModelProperty(value = "外部访问地址", hidden = true)
    @Column(columnDefinition = "varchar(150) COMMENT '外部访问地址'")
    private String outAddress;
    @ApiModelProperty(value = "内部访问地址", hidden = true)
    @Column(columnDefinition = "varchar(150) COMMENT '内部访问地址'")
    private String innerAddress;
    /**
     * pod属性
     */

    @ApiModelProperty(hidden = true)
    @Column(columnDefinition = "varchar(20) COMMENT '内部ip'")
    private String hostIP;
}
