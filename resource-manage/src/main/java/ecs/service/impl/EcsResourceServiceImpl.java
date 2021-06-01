package ecs.service.impl;

import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.Session;
import ecs.dao.CpuDao;
import ecs.dao.EcsResourceDao;
import ecs.dao.MemoryDao;
import ecs.entity.CPU;
import ecs.entity.EcsResource;
import ecs.entity.Memory;
import ecs.service.EcsResourceService;
import ecs.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static ecs.util.LinuxUtil.*;

@Slf4j
@EnableScheduling
@Service
public class EcsResourceServiceImpl implements EcsResourceService {
    @Autowired
    private EcsResourceDao ecsResourceDao;
    @Override
    public List<EcsResource> findAll(){
        return ecsResourceDao.findAll();
    }
    @Override
    public ResultVo add(EcsResource ecsResource) {
        ResultVo resultVo = new ResultVo();
        EcsResource byIp = ecsResourceDao.findByIp(ecsResource.getIp());
        if (byIp != null) {
            resultVo.setCode(-1);
            resultVo.setMsg("ip已经存在，不允许重复添加");
            return resultVo;
        }
        boolean reahable = false;
        try {
            reahable = ping(ecsResource.getIp()) ||
                    socket(ecsResource.getIp(), ecsResource.getRemotePort());
        } catch (Exception e) {
            e.printStackTrace();
            resultVo.setCode(-1);
            resultVo.setMsg("添加失败，未知错误");
            return resultVo;
        }
        if (!reahable) {
            log.info("目标主机不可达" + ecsResource.getIp());
            resultVo.setCode(-1);
            resultVo.setMsg("目标主机不可达");
            return resultVo;
        }
        if (ecsResource.getOperatingSystem().equalsIgnoreCase("linux")) {
            Session session = JschUtil.getSession(ecsResource.getIp(),
                    ecsResource.getRemotePort(), ecsResource.getAccount(), ecsResource.getPassword());
            String hostname = JschUtil.exec(session, "hostname", Charset.forName("UTF-8"));
            log.info("hostname=" + hostname);
            ecsResource.setHostname(hostname.trim());
            String cpu = JschUtil.exec(session, "cat /proc/cpuinfo | grep 'processor' | wc -l", Charset.forName("UTF-8"));
            ecsResource.setCpu(Integer.valueOf(cpu.trim()));
            String memory = JschUtil.exec(session, "cat /proc/meminfo | grep MemTotal | awk '{print $2}'", Charset.forName("UTF-8"));
            ecsResource.setMemory((int) (Long.valueOf(memory.trim()) / 1024 / 1024));
            ecsResource.setStatus("在线");
        }
        ecsResourceDao.save(ecsResource);
        resultVo.setCode(200);
        resultVo.setMsg("添加主机成功");
        return resultVo;
    }

    public ResultVo update(EcsResource ecsResource){
        EcsResource existsById = ecsResourceDao.findById(ecsResource.getId()).get();
        if(null == existsById){
            return ResultVo.renderErr(CodeEnum.QUERY_NOT_FOUND);
        }
        boolean reahable = false;
        try {
            reahable = ping(ecsResource.getIp()) || socket(ecsResource.getIp(), ecsResource.getRemotePort());
        } catch (Exception e) {
           return ResultVo.renderErr(CodeEnum.ERR).withRemark("添加失败，未知错误");
        }
        if (!reahable) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标主机不可达");
        }

        boolean verify = this.verify(ecsResource.getIp(), ecsResource.getPassword(), ecsResource.getRemotePort());
        if (!verify){
            return ResultVo.renderErr(CodeEnum.PASSWORD_WORRY);
        }
        ecsResource.setCreateDate(existsById.getCreateDate());
        ecsResource.setStatus(existsById.getStatus());
        ecsResource.setCreator(existsById.getCreator());
        ecsResource.setCreateDate(existsById.getCreateDate());

        Session session = JschUtil.getSession(ecsResource.getIp(),
                ecsResource.getRemotePort(), ecsResource.getAccount(), ecsResource.getPassword());
        String hostname = JschUtil.exec(session, "hostname", Charset.forName("UTF-8"));
        ecsResource.setHostname(hostname.trim());
        String cpu = JschUtil.exec(session, "cat /proc/cpuinfo | grep 'processor' | wc -l", Charset.forName("UTF-8"));
        ecsResource.setCpu(Integer.valueOf(cpu.trim()));
        String memory = JschUtil.exec(session, "cat /proc/meminfo | grep MemTotal | awk '{print $2}'", Charset.forName("UTF-8"));
        ecsResource.setMemory((int) (Long.valueOf(memory.trim())/1024/1024));
        ecsResourceDao.save(ecsResource);
        return ResultVo.renderOk();
    }

    @Override
    public ResultVo delete(Long id,String password){
        ResultVo resultVo = new ResultVo();
        boolean exist = ecsResourceDao.existsById(id);
        if (!exist){
            resultVo.setCode(-1);
            resultVo.setMsg("服务器不存在，删除失败");
            return resultVo;
        }
        EcsResource ecsResource = ecsResourceDao.findById(id).get();
        if (!ecsResource.getPassword().equals(password)){
            resultVo.setCode(-1);
            resultVo.setMsg("密码错误，删除失败！");
            return resultVo;
        }

        ecsResourceDao.delete(ecsResource);
        resultVo.setCode(200);
        resultVo.setMsg("删除成功！");
        return resultVo;
    }

    @Override
    public EcsResource findEcsResource(int status){
       return ecsResourceDao.findByStatus(status);
    }

    @Override
    public List<EcsResource> find(Map<String, Object> params, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        Specification<EcsResource> specification = new Specification<EcsResource>() {
            @Override
            public Predicate toPredicate(Root<EcsResource> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> {
                    list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%"));
                });
                return criteriaQuery.where(list.toArray(new Predicate[list.size()])).getRestriction();
            }
        };
        Page<EcsResource> pageObject = ecsResourceDao.findAll(specification, pageable);
        List<EcsResource> ecsResourceList = new ArrayList<>();
        pageObject.getContent().forEach(tmp -> {
            ecsResourceList.add(tmp);
        });
        return ecsResourceList;
    }

    @Override
    public long count(Map<String, Object> params) {
        Specification<EcsResource> specification = new Specification<EcsResource>() {
            @Override
            public Predicate toPredicate(Root<EcsResource> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> {
                    list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%"));
                });
                return criteriaQuery.where(list.toArray(new Predicate[list.size()])).getRestriction();
            }
        };
        return ecsResourceDao.count(specification);
    }

    /**
     * 查询服务器详情
     * @param id
     * @// TODO: 2020/8/19  
     * @return
     */
    @Override
    public ResultVo findDetail(Long id) {
        ResultVo resultVo = new ResultVo();
        boolean exists = ecsResourceDao.existsById(id);
        if (!exists){
            resultVo.setCode(-1);
            resultVo.setMsg("服务器不存在");
            return resultVo;
        }
        EcsResource ecsResource = ecsResourceDao.findById(id).get();

        return basicInfo(ecsResource.getIp());
    }

    @Override
    public boolean verify(String ip, String password,Integer remotePort) {
        Session session = null;
        try {
            session = JschUtil.getSession(ip,
                    remotePort, "root", password);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        if (session.isConnected()){
            return true;
        }
        return false;
    }

    @Override
    public ResultVo basicInfo(String ip) {
        EcsResource byIp = ecsResourceDao.findByIp(ip);
        if (null == byIp || Strings.isEmpty(byIp.getPassword())) return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标服务器信息不存在或者密码不存在");
        String hostname = byIp.getHostname();
        String operatingSystem = byIp.getOperatingSystem();
        Session session = JschUtil.getSession(ip,byIp.getRemotePort(),byIp.getAccount(), byIp.getPassword());
        String systemVersion = JschUtil.exec(session, "uname -a", Charset.forName("UTF-8"));
        String ifconfig = JschUtil.exec(session, "ifconfig  -a | awk -F '     ' '{print $1}' | tr -s '\\n' | wc -l", Charset.forName("UTF-8"));
        String cpus = JschUtil.exec(session, "lscpu | sed -n 4p | awk '{print $2}'", Charset.forName("UTF-8"));
        Map<String, String> result = new HashMap(){{
            put("hostname", hostname.replace("\n",""));
            put("operatingSystem", operatingSystem.replace("\n",""));
            put("systemVersion", systemVersion.replace("\n",""));
            put("ifconfig", ifconfig.replace("\n",""));
            put("cpus", cpus.replace("\n",""));
            put("ip", ip.replace("\n",""));
        }};
        return ResultVo.renderOk(result).withRemark("查询基本信息成功！");
    }

    @Override
    public ResultVo processList(String ip) {
        EcsResource byIp = ecsResourceDao.findByIp(ip);
        if (null == byIp || Strings.isEmpty(byIp.getPassword())) return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标服务器信息不存在或者密码不存在");
        Session session = JschUtil.getSession(ip,byIp.getRemotePort(),byIp.getAccount(), byIp.getPassword());
        String exec = JschUtil
                .exec(session, "ps aux --sort=-pcpu | head -10",
                        Charset.forName("UTF-8"));
        String[] strings = exec.split("\n");

        List<ProcessVo> processVoList = new ArrayList<>();
        for (int i=1; i< strings.length; i++){
            String[] split = strings[i].split("\\s+");
            ProcessVo vo = new ProcessVo();
            vo.setUser(split[0]);
            vo.setPid(split[1]);
            vo.setCpu(split[2]);
            vo.setMem(split[3]);
            vo.setVsz(split[4]);
            vo.setRss(split[5]);
            vo.setTty(split[6]);
            vo.setStat(split[7]);
            vo.setStart(split[8]);
            vo.setTime(split[9]);
            vo.setCommand(split[10]);
            processVoList.add(vo);
        }
        return ResultVo.renderOk(processVoList).withRemark("查询成功！");
    }

    @Override
    public ResultVo diskInfo(String ip) {
        EcsResource byIp = ecsResourceDao.findByIp(ip);
        if (null == byIp || Strings.isEmpty(byIp.getPassword())) return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标服务器信息不存在或者密码不存在");
        Session session = JschUtil.getSession(ip,byIp.getRemotePort(),byIp.getAccount(), byIp.getPassword());
        String exec = JschUtil.exec(session, "lsblk -l | grep lvm", Charset.forName("UTF-8"));
        List<DiskVo> diskVoList = new ArrayList<>();
        String[] strings = exec.split("\n");
        DiskVo vo = null;
        for (int i=0; i< strings.length; i++){
            vo = new DiskVo();
            String[] split = strings[i].split("\\s+");
            String name = split[0].trim();
            String exec1 = JschUtil.exec(session, "df -h | grep " + name, Charset.forName("UTF-8"));
            if (Strings.isNotEmpty(exec1)){
                String[] split1 = exec1.split("\\s+");
                vo.setFileSystem(split1[0]);
                vo.setSize(split1[1]);
                vo.setUsed(split1[2]);
                vo.setAvail(split1[3]);
                vo.setUsePercent(split1[4].replace("%",""));
                if (!diskVoList.contains(vo)){
                    diskVoList.add(vo);
                }

            }
        }
        return ResultVo.renderOk(diskVoList);
    }

    @Override
    public ResultVo ethInfo(String ip) {
        EcsResource byIp = ecsResourceDao.findByIp(ip);
        if (null == byIp || Strings.isEmpty(byIp.getPassword())){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标服务器信息不存在或者密码不存在");
        }
        Session session = JschUtil.getSession(ip,byIp.getRemotePort(),byIp.getAccount(), byIp.getPassword());
        String exec = JschUtil
                .exec(session, "ifconfig  | grep -w inet | awk {'print $2,$4,$6'}", Charset.forName("UTF-8"));
        String[] strings = exec.split("\n");
        List<InetVo> inetVoList = new ArrayList<>();
        InetVo inetVo = null;
        for (int i=0; i<strings.length; i++){
            inetVo = new InetVo();
            String[] split = strings[i].split("\\s+");
            try {
                inetVo.setInet(split[0]);
                inetVo.setNetmask(split[1]);
                inetVo.setBroadcast(split[2]);
            }catch (ArrayIndexOutOfBoundsException e){

            }finally {
                inetVoList.add(inetVo);
            }
        }
        return ResultVo.renderOk(inetVoList);
    }

    @Override
    public ResultVo portsListen(String ip) {
        return null;
    }

    @Autowired
    CpuDao cpuDao;

    @Autowired
    MemoryDao memoryDao;

    @Override
    public ResultVo cpuInfo(String ip, String rangeType) {
        EcsResource byIp = ecsResourceDao.findByIp(ip);
        if (null == byIp || Strings.isEmpty(byIp.getPassword())){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标服务器信息不存在或者密码不存在");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        List<CPU> list = new ArrayList<>();
        switch (rangeType){
            case "DAY":
                calendar1.set(Calendar.HOUR_OF_DAY, calendar1.get(Calendar.HOUR_OF_DAY) - 24);
                calendar2.set(Calendar.HOUR_OF_DAY, calendar2.get(Calendar.HOUR_OF_DAY));
                list = cpuDao.findByIpAndDate(ip, sdf.format(calendar1.getTime()) , sdf.format(calendar2.getTime()));
                break;
            case "WEEK":
                calendar1.set(Calendar.DAY_OF_YEAR, calendar1.get(Calendar.DAY_OF_YEAR) - 7);
                calendar2.set(Calendar.DAY_OF_YEAR, calendar2.get(Calendar.DAY_OF_YEAR));
                list = cpuDao.findByIpAndDate(ip, sdf.format(calendar1.getTime()) , sdf.format(calendar2.getTime()));
                break;
            case "MONTH":
                calendar1.set(Calendar.DAY_OF_YEAR, calendar1.get(Calendar.DAY_OF_YEAR) - 30);
                calendar2.set(Calendar.DAY_OF_YEAR, calendar2.get(Calendar.DAY_OF_YEAR));
                list = cpuDao.findByIpAndDate(ip, sdf.format(calendar1.getTime()) , sdf.format(calendar2.getTime()));
                break;
            default:
                break;
        }

        List<CpuVO> vos = new ArrayList<CpuVO>();
        list.forEach(tmp ->{
            CpuVO vo = new CpuVO();
            vo.setAverage1(tmp.getAverage1());
//            vo.setAverage2(tmp.getAverage2());
//            vo.setAverage3(tmp.getAverage3());
            vo.setDate(tmp.getDate());
            vos.add(vo);
        });
        return ResultVo.renderOk(vos).withRemark("查询cpu历史负载成功！");
    }

    @Override
    public ResultVo memoryInfo(String ip, String rangeType) {
        EcsResource byIp = ecsResourceDao.findByIp(ip);
        if (null == byIp || Strings.isEmpty(byIp.getPassword())){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标服务器信息不存在或者密码不存在");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        List<Memory> list = new ArrayList<>();
        switch (rangeType){
            case "DAY":
                calendar1.set(Calendar.HOUR_OF_DAY, calendar1.get(Calendar.HOUR_OF_DAY) - 24);
                calendar2.set(Calendar.HOUR_OF_DAY, calendar2.get(Calendar.HOUR_OF_DAY));
                list = memoryDao.findByIpAndDate(ip, sdf.format(calendar1.getTime()) , sdf.format(calendar2.getTime()));
                break;
            case "WEEK":
                calendar1.set(Calendar.DAY_OF_YEAR, calendar1.get(Calendar.DAY_OF_YEAR) - 7);
                calendar2.set(Calendar.DAY_OF_YEAR, calendar2.get(Calendar.DAY_OF_YEAR));
                list = memoryDao.findByIpAndDate(ip, sdf.format(calendar1.getTime()) , sdf.format(calendar2.getTime()));
                break;
            case "MONTH":
                calendar1.set(Calendar.DAY_OF_YEAR, calendar1.get(Calendar.DAY_OF_YEAR) - 30);
                calendar2.set(Calendar.DAY_OF_YEAR, calendar2.get(Calendar.DAY_OF_YEAR));
                list = memoryDao.findByIpAndDate(ip, sdf.format(calendar1.getTime()) , sdf.format(calendar2.getTime()));
                break;
            default:
                break;
        }
        return ResultVo.renderOk(list).withRemark("查询内存历史负载成功！");
    }

    /**
     * 定时统计cpu负载 15分钟间隔
     */
    @Autowired
    ExecutorService executorService;

    @Scheduled(fixedRate = 1000*60*15)
    public void cpuAndMemorySchedule() throws ExecutionException, InterruptedException {
        log.info("开始计算cpu负载");
        List<EcsResource> ecsResourceList = ecsResourceDao.findAll();
        List<CPU> cpuList = new ArrayList<>();
        List<Future<CPU>> futures = new ArrayList<>();
        for (EcsResource ecsResource : ecsResourceList){
            try {
                futures.add(executorService.submit(new UptimeThread(ecsResource)));
            }catch (Exception e){

            }
        }
        for (Future<CPU> cpuFuture : futures){
            cpuList.add(cpuFuture.get());
        }
        cpuDao.saveAll(cpuList);
        log.info("cpu负载计算结束");

        log.info("开始计算内存负载");
        List<Memory> memoryList = new ArrayList<>();
        List<Future<Memory>> futures1 = new ArrayList<>();
        for (EcsResource ecsResource : ecsResourceList){
            try {
                futures1.add(executorService.submit(new FreeThread(ecsResource)));
            }catch (Exception e){

            }
        }
        for (Future<Memory> memoryFuture: futures1){
            memoryList.add(memoryFuture.get());
        }
        memoryDao.saveAll(memoryList);
        log.info("内存负载计算完成");

        log.info("开始检测服务器在线情况");
        List<Future<EcsResource>> futures2 = new ArrayList<>();
        for (EcsResource ecsResource : ecsResourceList){
            futures2.add(executorService.submit(new OnlineThread(ecsResource)));
        }
        for (Future<EcsResource> ecsResourceFuture : futures2){
            ecsResourceList.add(ecsResourceFuture.get());
        }
        log.info("检测服务器在线情况完成");

    }
}

/**
 * 多线程检测在线
 */
class OnlineThread implements Callable<EcsResource>{
    private EcsResource ecsResource;
    private String ip;
    private Integer remotePort;

    public OnlineThread(EcsResource ecsResource){
        this.ecsResource = ecsResource;
        this.ip = ecsResource.getIp();
        this.remotePort = ecsResource.getRemotePort();
    }
    @Override
    public EcsResource call() throws Exception {
        boolean reahable = ping(ip) || socket(ip,remotePort);
        ecsResource.setStatus(reahable ? "在线" : "离线");
        return ecsResource;
    }
}

/**
 * 多线程处理
 */
class UptimeThread implements Callable<CPU>{

    private CPU cpu;
    private Date date;
    private EcsResource ecsResource;
    private Session session;

    public UptimeThread(EcsResource ecsResource) {
        this.ecsResource = ecsResource;
        this.date = new Date();
        this.cpu = new CPU();
        this.session = JschUtil.getSession(ecsResource.getIp(),ecsResource.getRemotePort(),
                ecsResource.getAccount(), ecsResource.getPassword());
    }

    @Override
    public CPU call() throws Exception {
        String uptime = JschUtil.exec(session, "uptime ", Charset.forName("UTF-8"));
        String average = uptime.substring(uptime.indexOf("average")+8);
        String days = "1";
        try{
            days = uptime.substring(uptime.indexOf("up")+2, uptime.indexOf("days"));
        }catch (Exception e){
            
        }
        String[] split = average.split(",");
        cpu.setIp(ecsResource.getIp());
        cpu.setDate(date);
        cpu.setDays(Integer.valueOf(days.trim()));
        cpu.setHostName(ecsResource.getHostname());
        cpu.setAverage1(Double.parseDouble(split[0].trim()));
        cpu.setAverage2(Double.parseDouble(split[1].trim()));
        cpu.setAverage3(Double.parseDouble(split[2].trim()));
        return cpu;
    }
}

/**
 * 统计内存
 */
class FreeThread implements Callable<Memory> {

    private Memory memory;
    private Date date;
    private EcsResource ecsResource;
    private Session session;

    public FreeThread(EcsResource ecsResource) {
        this.ecsResource = ecsResource;
        this.date = new Date();
        this.memory = new Memory();
        this.session = JschUtil.getSession(ecsResource.getIp(),ecsResource.getRemotePort(),
                ecsResource.getAccount(), ecsResource.getPassword());
    }

    @Override
    public Memory call() throws Exception {
        String shell = JschUtil.exec(session, "free -m | grep Mem | awk '{print $2,$3,$4,$5,$6,$7}'",
                Charset.forName("UTF-8"));
        String[] split = shell.split("\\s+");
        memory.setDate(date);
        memory.setHostName(ecsResource.getHostname());
        memory.setIp(ecsResource.getIp());
        memory.setTotal(Long.valueOf(split[0].trim()));
        memory.setUsed(Long.valueOf(split[1].trim()));
        memory.setFree(Long.valueOf(split[2].trim()));
        memory.setShared(Long.valueOf(split[3].trim()));
        memory.setBuff_cache(Long.valueOf(split[4].trim()));
        memory.setAvailable(Long.valueOf(split[5]));
        return memory;
    }
}