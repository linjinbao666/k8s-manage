package monitor.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class K8sResourceVo {
    private String name;
    private double all;
    private double used;
    private double avaliable;
    private double percentage;
}
