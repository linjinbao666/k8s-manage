package ecs.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * cpu负载画图
 */
@Getter
@Setter
@ToString
public class CpuVO implements Serializable {
    private Date date;
    private double average1;
//    private double average2;
//    private double average3;
}
