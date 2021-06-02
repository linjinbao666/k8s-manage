package monitor.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import monitor.entity.IPRule;
import monitor.service.IPRuleService;
import monitor.vo.ResultVo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api("黑白名单接口")
@RestController
@RequestMapping("/ipRule")
public class IPRuleController {

    @Autowired
    IPRuleService ipRuleService;

    @ApiOperation("查询黑名单")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "名称", required = false),
            @ApiImplicitParam(name = "appName", value = "应用名称", required = false),
            @ApiImplicitParam(name = "type", value = "类型，0 白名单 1 黑名单", required = false),
            @ApiImplicitParam(name = "status", value = "状态，0 禁用 1 启用"),
            @ApiImplicitParam(name = "pageNum", value = "分页页码"),
            @ApiImplicitParam(name = "pageSize", value = "分页大小"),
    })
    @GetMapping("/")
    public ResultVo rule(
            @RequestParam(value = "name", required = false)String name,
            @RequestParam(value = "appName", required = false)String appName,
            @RequestParam(value = "type", required = false)String type,
            @RequestParam(value = "status", required = false)String status,
            @RequestParam(value = "pageNum",required = false, defaultValue = "1")Integer pageNum,
            @RequestParam(value = "pageSize",required = false, defaultValue = "5")Integer pageSize
    ){
        Map params = new HashMap(){{
            if (Strings.isNotEmpty(name)) put("name", name);
            if (Strings.isNotEmpty(appName)) put("appName", appName);
            if (Strings.isNotEmpty(type)) put("type", type);
            if (Strings.isNotEmpty(status)) put("status", status);
        }};
        List items =  ipRuleService.findAll(params, pageNum-1, pageSize);
        long count = ipRuleService.count(params);
        return ResultVo.renderOk(new HashMap<String, Object>(){{
            put("items", items);
            put("count", count);
        }}).withRemark("查找所有规则");
    }

    @ApiOperation("增加规则")
    @PostMapping("/")
    public ResultVo addRule(@Validated IPRule ipRule){
        return ipRuleService.addOne(ipRule);
    }

    @ApiOperation("修改规则")
    @ApiImplicitParam(name = "id", value = "记录id", required = true)
    @PostMapping("/updateRule")
    public ResultVo updateRule(
            @RequestParam(value = "id")Long id,
            @RequestParam(value = "type")Integer type,
            @RequestParam(value = "rule")String rule,
            @RequestParam(value = "status")Integer status
    ){
        return ipRuleService.updateOne(id, type, rule, status);
    }

    @ApiOperation("删除")
    @ApiImplicitParam(name = "id", value = "记录id", required = true)
    @DeleteMapping("/{id}")
    public ResultVo deleteRule(
            @PathVariable(value = "id")Long id
    ){
        return ipRuleService.deleteOne(id);
    }
}
