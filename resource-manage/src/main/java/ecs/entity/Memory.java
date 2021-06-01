package ecs.entity;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Getter
@Setter
@ToString
@Table(name = "ecs_memory")
@EntityListeners(AuditingEntityListener.class)
public class Memory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;        //id
    private Date date;      //统计时间
    private String ip;      //ip地址
    private String hostName;//主机名称

    private long total;
    private long used;
    private long free;
    private long shared;
    private long buff_cache;
    private long available;

}
