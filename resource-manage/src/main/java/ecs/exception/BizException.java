package ecs.exception;

import ecs.vo.CodeEnum;
import ecs.vo.ResultVo;
import lombok.Data;

/**
 * 业务异常跳转。
 */
@Data
public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private CodeEnum code;
    private String remark;

    public BizException(CodeEnum code) {
        super(code.getMessage());
        this.code = code;
    }

    public BizException withRemark(String remark) {
        this.remark = remark;
        return this;
    }
}
