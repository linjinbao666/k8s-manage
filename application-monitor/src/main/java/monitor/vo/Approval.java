package monitor.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 审批信息
 *
 * @author zhuzl
 * @date 2020年8月22日
 *
 */

@Getter
@Setter
@Data
@ToString
public class Approval implements Serializable {

    /**
     * 数据库自增主键
     */
    private long id;
    /**
     * 业务主键
     */
    private String approvalKey;
    /**
     * 申请/审核的业务类型
     */
    private String businessType;
    /**
     * 审批对象的名称
     */
    private String objectName;
    /**
     * 审批对象的描述信息
     */
    private String objectMemo;
    /**
     * 详情
     */
    private String detailMessage;

    /**
     * 申请人ID
     */
    private long applicantUserId;
    /**
     * 审批记录的状态
     */
    private String state;

}
