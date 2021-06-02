package ecs.controller;

import ecs.entity.ResourceRequest;
import ecs.entity.SubCenter;
import ecs.service.ResourceRequestService;
import ecs.util.MyUtil;
import ecs.vo.CodeEnum;
import ecs.vo.ResultVo;
import ecs.vo.UserContext;
import io.swagger.annotations.*;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.Cookie;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "厂商管理")
@RestController
@RequestMapping(value = "/resourceRequest")
public class ResourceController {
    @Autowired
    private ResourceRequestService resourceRequestService;
    @ApiOperation(value = "获取厂商列表", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "厂商名称",name = "companyName", required = false),
            @ApiImplicitParam(value = "组织id",name = "regId", required = false),
            @ApiImplicitParam(value = "名称空间", name = "namespace", required = false)
    })
    @ApiResponse(code = 200, message = "查询厂商列表成功")
    @GetMapping(value = "/namespace")
    public ResultVo getNamespaces(
            @RequestParam(value = "companyName", required = false) String companyName,
            @RequestParam(value = "regId", required = false) String regId,
            @RequestParam(value = "namespace", required = false) String namespace
    ){
        Map<String, Object> params = new HashMap<>();
        if(Strings.isNotEmpty(companyName)){
            params.put("companyName", companyName);
        }
        if(Strings.isNotEmpty(regId)){
            params.put("regId", regId);
        }
        if(Strings.isNotEmpty(companyName)){
            params.put("namespace", namespace);
        }

        List<ResourceRequest> all = resourceRequestService.findAll(params);

        return ResultVo.renderOk(all).withRemark("查询厂商列表成功");
    }

    @ApiOperation(value = "厂商申请空间", httpMethod = "POST")
    @ApiResponses({@ApiResponse(code = 200, message = "申请资源成功")})
    @PostMapping(value = "/namespace")
    public ResultVo createResourceRequest(@Validated ResourceRequest resourceRequest){
        if (Strings.isEmpty(resourceRequest.getCompanyName())) {
            resourceRequest.setCompanyName(resourceRequest.getNamespace());
        }
        return resourceRequestService.createResourcesRequest(resourceRequest);
    }

    @ApiOperation(value = "查询厂商空间详情", httpMethod = "GET")
    @ApiImplicitParam(paramType = "path", name = "名称空间",value = "namespace", required = true)
    @ApiResponse(code = 200, message = "查询成功！")
    @GetMapping(value = "/namespace/{namespace}")
    public ResultVo getNamespaceDetail(@PathVariable("namespace")String namespace){
        ResourceRequest resourceRequest = resourceRequestService.findNamespace(namespace);
        return ResultVo.renderOk(resourceRequest);
    }

    @ApiOperation(value = "删除厂商空间", httpMethod = "DELETE")
    @DeleteMapping(value = "/namespace/{namespace}")
    public ResultVo deleteNamespaces(
            @PathVariable(value = "namespace") String namespace
    ) {
        return resourceRequestService.deleteNamespace(namespace);
    }

    @ApiOperation("修改厂商资源信息")
    @PostMapping(value = "/updateResourcesRequest")
    public ResultVo updateResourcesRequest(@Validated ResourceRequest resourceRequest){
        if (Strings.isEmpty(resourceRequest.getCompanyName())) {
            resourceRequest.setCompanyName(resourceRequest.getNamespace());
        }
        return resourceRequestService.updateResourcesRequest(resourceRequest);
    }

    @ApiOperation("同步服务器namespace到数据库 ")
    @GetMapping("/sync")
    public ResultVo sync(){
        return resourceRequestService.sync();
    }

    @ApiOperation("创建分中心")
    @PostMapping("/createSubCenter")
    public ResultVo createSubCenter(@Validated SubCenter subCenter){
        return resourceRequestService.createSubCenter(subCenter);
    }

    @ApiOperation("查询分中心列表")
    @ApiImplicitParam(name = "centerName", value = "分中心名称")
    @GetMapping("/findsubCenters")
    public ResultVo findsubCenters(
            @RequestParam(value = "centerName", required = false) String centerName){

        return resourceRequestService.findSubCenter(centerName);
    }

    @ApiOperation("删除分中心")
    @DeleteMapping("/deleteSubCenter")
    public ResultVo deleteSubCenter(long id){
        return resourceRequestService.deleteSubCenter(id);
    }

    @Autowired
    private RestTemplate restTemplate;
    @Value("${approval.gateway}")
    private String gateway;

    @ApiOperation("查询组织信息")
    @GetMapping(value = "/findRegsAllAsynCen", produces="application/json;charset=UTF-8")
    public String findRegsAllAsynCen(@RequestParam(value = "name", required = false)String name,
                                     @RequestParam(value = "parentId", defaultValue = "0", required = false) long parentId){
        UserContext userContext = UserContext.getUserContext();
        HttpHeaders headers = new HttpHeaders();
        for (int i=0; i< userContext.getCookies().length; i++) {
            Cookie cookie = userContext.getCookies()[i];
            headers.add("Cookie", cookie.getName() + "=" + cookie.getValue());
        }
        MultiValueMap<String, Object> map= new LinkedMultiValueMap<String, Object>();
        map.add("name",name);
        map.add("parentId",parentId);
        MediaType type = MediaType.parseMediaType("application/json;charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());

        HttpEntity request = new HttpEntity(headers);
        ResponseEntity<String> exchange = restTemplate.exchange(gateway
                +"/workcenter/api/v1/registration/findRegsAllAsynCen"
                +"?name="+name+"&parentId="+parentId
                ,HttpMethod.GET,request, String.class);

        int status = exchange.getStatusCodeValue();
        if (status != 200){
           return "获取数据失败";
        }
        String body = exchange.getBody();
        return body;
    }

}
