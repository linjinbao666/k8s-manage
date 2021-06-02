package ecs.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 审核事件的详细信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDetailVo implements Serializable {

    private String className; //类名
    private String methodName; //方法名
    private String paramType; //参数类型
    private String paramsJsonStr;  //参数列表
}
