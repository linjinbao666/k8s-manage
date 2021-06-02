package ecs.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * cpu负载历史统计
 */
@Data
@Entity
@Getter
@Setter
@ToString
@Table(name = "ecs_uptime")
@EntityListeners(AuditingEntityListener.class)
public class CPU implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;        //id
    private String ip;      //ip地址
    private String hostName;//主机名称
    private Integer users;  //用户数量
    private Integer days;   /**运行时常**/
    private double average1;/**1分钟**/
    private double average2;/**5分钟**/
    private double average3;/**15分钟**/
    private Date date;      //统计时间

}
