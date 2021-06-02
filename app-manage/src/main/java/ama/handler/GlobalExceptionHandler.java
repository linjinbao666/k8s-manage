package ama.handler;
import ama.exception.BizException;
import ama.enumlation.CodeEnum;
import ama.vo.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ValidationException;
import java.util.StringJoiner;

/**
 * 全局异常处理
 * <p>
 * 规范：流程跳转尽量避免使用抛 BizException 来做控制。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(BizException.class)
    public ResultVo handleBizException(BizException ex) {
        ResultVo result = ResultVo.renderErr(ex.getCode());
        if (!StringUtils.isEmpty(ex.getRemark())) {
            result.withRemark(ex.getRemark());
        }
        return result;
    }

    /**
     * 参数绑定错误
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(BindException.class)
    public ResultVo handleBindException(BindException ex) {
        StringJoiner sj = new StringJoiner(";");
        ex.getBindingResult().getFieldErrors().forEach(x -> sj.add(x.getDefaultMessage()));
        return ResultVo.renderErr(CodeEnum.REQUEST_ERR).withRemark(sj.toString());
    }

    /**
     * 参数校验错误
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(ValidationException.class)
    public ResultVo handleValidationException(ValidationException ex) {
        return ResultVo.renderErr(CodeEnum.REQUEST_ERR).withRemark(ex.getCause().getMessage());
    }

    /**
     * 字段校验不通过异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVo handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        StringJoiner sj = new StringJoiner(";");
        ex.getBindingResult().getFieldErrors().forEach(x -> sj.add(x.getDefaultMessage()));
        return ResultVo.renderErr(CodeEnum.REQUEST_ERR).withRemark(sj.toString());
    }

    /**
     * Controller参数绑定错误
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResultVo handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return ResultVo.renderErr(CodeEnum.REQUEST_ERR).withRemark(ex.getMessage());
    }

    /**
     * 处理方法不支持异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public ResultVo handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return ResultVo.renderErr(CodeEnum.METHOD_NOT_ALLOWED);
    }

    /**
     * 其他未知异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public ResultVo handleException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return ResultVo.renderErr(CodeEnum.SERVER_ERR);
    }
}