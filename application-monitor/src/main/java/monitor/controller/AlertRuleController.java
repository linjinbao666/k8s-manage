package monitor.controller;

import io.swagger.annotations.*;
import monitor.entity.AlertHistory;
import monitor.entity.AlertRule;
import monitor.service.AlertRuleService;
import monitor.vo.ResultVo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警管理接口
 */

@Api("告警管理")
@RestController
@RequestMapping("/alertRule")
public class AlertRuleController {

    @Autowired
    AlertRuleService alertRuleService;

    @ApiOperation("查询所有规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "alertName", value = "告警名曾"),
            @ApiImplicitParam(name = "appName", value = "应用名称"),
            @ApiImplicitParam(name = "target", value = "指标"),
            @ApiImplicitParam(name = "status", value = "状态"),
            @ApiImplicitParam(name = "pageNum", value = "分页页码"),
            @ApiImplicitParam(name = "pageSize", value = "分压大小"),
    })
    @GetMapping("/")
    public ResultVo rule(
            @RequestParam(value = "alertName", required = false)String alertName,
            @RequestParam(value = "appName", required = false)String appName,
            @RequestParam(value = "target", required = false)String target,
            @RequestParam(value = "status", required = false)String status,
            @RequestParam(value = "pageNum",required = false, defaultValue = "1")Integer pageNum,
            @RequestParam(value = "pageSize",required = false, defaultValue = "5")Integer pageSize
    ){

        Map params = new HashMap();
        if (Strings.isNotEmpty(alertName)) params.put("alertName", alertName);
        if (Strings.isNotEmpty(appName)) params.put("appName", appName);
        if (Strings.isNotEmpty(target)) params.put("target", target);
        if (Strings.isNotEmpty(status)) params.put("status", status);

        List items =  alertRuleService.findAll(params, pageNum-1, pageSize);
        long count = alertRuleService.count(params);

        return ResultVo.renderOk(new HashMap<String, Object>(){{
            put("items", items);
            put("count", count);
        }}).withRemark("查找所有规则");
    }

    @ApiOperation(("根据id查询详情"))
    @GetMapping("/findOne")
    public ResultVo findOne(@RequestParam("id")long id){
        return alertRuleService.findOne(id);
    }

    @ApiOperation("添加一条新的规则")
    @PostMapping("/")
    @ApiResponses({
            @ApiResponse(code = 200, message = "增加成功！")
    })
    public ResultVo rule(@Validated AlertRule alertRule){
        if (Strings.isEmpty(alertRule.getUserName())) {
            alertRule.setUserName("admin");
        }
        return alertRuleService.addOne(alertRule);
    }

    @ApiOperation("查询用户列表")
    @GetMapping("/users")
    public ResultVo users(){
        List<Map<String, String>> userList = new ArrayList<>();
        userList.add(new HashMap<String, String>(){{
            put("id","1029303");
            put("name","admin");
        }});
        userList.add(new HashMap<String, String>(){{
            put("id","1029304");
            put("name","admin2");
        }});
        userList.add(new HashMap<String, String>(){{
            put("id","1029305");
            put("name","admin3");
        }});
        return ResultVo.renderOk(userList).withRemark("查询用户列表成功！");
    }

    @ApiOperation("修改告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "记录id", required = true, dataType = "Long"),
            @ApiImplicitParam(name = "target", value = "指标"),
            @ApiImplicitParam(name = "quota", value = "阈值"),
            @ApiImplicitParam(name = "status", value = "状态"),
    })
    @PostMapping("/updateOne")
    @ApiResponses({
            @ApiResponse(code = 200, message = "修改成功！")
    })
    public ResultVo updateRule(
            @RequestParam(value = "id") Long id,
            @RequestParam(value = "target", required = false)String target,
            @RequestParam(value = "quota", required = false)String quota,
            @RequestParam(value = "status", required = false)Integer status
    ){
        return alertRuleService.updateOne(id, target, quota, status);
    }

    @ApiOperation("删除")
    @ApiImplicitParam(name = "id", value = "记录id", required = true)
    @DeleteMapping("/{id}")
    public ResultVo deleteRule(
            @PathVariable(value = "id")Long id
    ){
        return alertRuleService.deleteOne(id);
    }

    @ApiOperation("查询历史记录")
    @ApiImplicitParam(name = "ruleId", value = "记录id", required = true)
    @GetMapping("/history")
    public ResultVo history(
            long ruleId,
            @RequestParam(value = "pageNum")Integer pageNum,
            @RequestParam(value = "pageSize")Integer pageSize
    ){
        List<AlertHistory> history = alertRuleService.history(ruleId, pageNum-1, pageSize);
        Map map = new HashMap(){{
            put("items", history);
            put("count", alertRuleService.historyCount(ruleId));
        }};

        return ResultVo.renderOk(map).withRemark("查询历史成功");
    }

}
