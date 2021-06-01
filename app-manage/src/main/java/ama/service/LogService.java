package ama.service;

import ama.vo.ResultVo;
import cn.hutool.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;

/**
 * 日志分析服务
 */
public interface LogService {
    /**
     * 统计概览
     * @return
     */
    ResultVo conutSummary(String namespace) throws IOException;

    /**
     * 根据时间统计
     * @return
     */
    ResultVo countByTime(String namespace, String date,String startTime, String endTime, String labels, String appName) throws ParseException, IOException;

    /**
     * 根据应用统计
     * @return
     */
    ResultVo countByApp(String namespace) throws IOException;
}
