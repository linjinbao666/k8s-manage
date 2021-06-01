package monitor.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;


@Getter
@Setter
@Data
public class AlertInfo implements Serializable {

    /**
     * 报警名称
     */
    private String alertName;
    /**
     * 报警类型
     */
    private String alertType;

    /**
     * 阈值
     */
    private String alertRate;
    /**
     * 报警描述
     */
    private String alertDesc;
    /**
     * 创建时间
     */
    private Date createDate;


    /**
     * 收件邮箱
     */
    private String receiveEmail;


    /**
     * 应用名称
     */
    private String appName;
}
