package monitor.exception;

import lombok.Data;
import monitor.enumlation.CodeEnum;

/**
 * 业务异常跳转。
 */
@Data
public class BizException extends RuntimeException {
    private static final long serialVersionUID = -2257339086274759935L;

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
