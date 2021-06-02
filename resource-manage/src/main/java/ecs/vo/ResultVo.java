package ecs.vo;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ResultVo<T> {
    private Integer code = CodeEnum.OK.getCode();
    private String msg = CodeEnum.OK.getMessage();
    private T data;
    private Long ttl;

    public ResultVo(CodeEnum code) {
        this.setCode(code.getCode());
        this.setMsg(code.getMessage());
    }

    public ResultVo(T data) {
        CodeEnum code = CodeEnum.OK;
        this.setCode(code.getCode());
        this.setMsg(code.getMessage());
        this.setData(data);
    }

    @JsonIgnore
    public boolean isOk() {
        return CodeEnum.OK.getCode().equals(this.code);
    }

    /**
     * 附加信息
     *
     * @param remark
     * @return
     */
    public ResultVo withRemark(String remark) {
        this.setMsg(this.getMsg() + "(" + remark + ")");
        return this;
    }

    /**
     * 构建失败返回结果
     *
     * @param code 状态码
     * @return 失败返回结果
     */
    public static ResultVo renderErr(CodeEnum code) {
        return new ResultVo(code);
    }

    /**
     * 构建无效操作返回结果
     *
     * @return
     */
    public static ResultVo renderVain() {
        return new ResultVo(CodeEnum.INVALID_OPERATION);
    }

    /**
     * 构建成功返回结果
     *
     * @return
     */
    public static ResultVo renderOk() {
        return new ResultVo(CodeEnum.OK);
    }

    /**
     * 构建成功返回结果(带数据)
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> ResultVo<T> renderOk(T data) {
        return new ResultVo<>(data);
    }

    public ResultVo withTTL(long execTimeMillis) {
        this.setTtl(ttl);
        return this;
    }
}