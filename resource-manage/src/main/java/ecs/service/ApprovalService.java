package ecs.service;

import ecs.vo.Approval;
import ecs.vo.ResultVo;

/**
 * 审核
 */
public interface ApprovalService {

    public void execute(Approval approval);

    /**
     * 发送审核
     * @param approval
     * @return
     */
    public ResultVo send2Approval(Approval approval);

    /**
     * 回写执行结果
     * @param approvalKey
     * @param message
     * @param success
     */
    public void sendResult(String approvalKey, String message, String success);
}
