package ecs.service.impl;

import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import ecs.dao.EcsResourceDao;
import ecs.dao.PvcDao;
import ecs.entity.EcsResource;
import ecs.entity.Pvc;
import ecs.service.PvcService;
import ecs.upload.FileProgressMonitor;
import ecs.vo.CodeEnum;
import ecs.vo.LsEntryVo;
import ecs.vo.ResultVo;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

@Service
public class PvcServiceImpl implements PvcService {
    private static final Logger LOG = LoggerFactory.getLogger(PvcService.class);

    private static  Sftp sftp = null;

    @Autowired
    private PvcDao pvcDao;
    @Autowired
    private EcsResourceDao ecsResourceDao;
    @Autowired
    KubernetesClient client;
    @Override
    public List<Pvc> findAll(){
        return pvcDao.findAll();
    }

    @Override
    public ResultVo createPvc(Pvc pvc) {
        List<Namespace> items = client.namespaces().list().getItems();
        Namespace namespace = items.stream().filter(ns -> pvc.getNamespace().equals(ns.getMetadata().getName())).findAny().orElse(null);
        if (null == namespace) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("该名称空间不存在");
        }
        List<Pvc> allClaims = this.findAll();
        for (Pvc p : allClaims){
            if (p.getNamespace().equals(pvc.getNamespace()) && p.getPvcName().equals(pvc.getPvcName())){
               return ResultVo.renderErr(CodeEnum.ERR).withRemark("同一名称空间下不允许创建同名持久化卷");
            }
        }
        Map<String, Quantity> requests = new HashMap<>();
        Quantity storage = new QuantityBuilder()
                .withAmount(String.valueOf(pvc.getCapaCity()))
                .withFormat("Mi")
                .build();
        requests.put("storage", storage);

        List<StorageClass> scs = client.storage().storageClasses().list().getItems();
        if (scs.isEmpty()) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("请确认nfs组件是否正确部署");
        }
        String nfsName = "nfs";
        for (StorageClass sc : scs){
            if (sc.getProvisioner().contains("fuseim.pri/ifs")) {
                nfsName = sc.getMetadata().getName();
                break;
            }
        }
        PersistentVolumeClaim persistentVolumeClaim = new PersistentVolumeClaimBuilder()
                .withApiVersion("v1")
                .withKind("PersistentVolumeClaim")
                .withNewMetadata().withName(pvc.getPvcName())
                .withNewNamespace(pvc.getNamespace())
                .endMetadata()
                .withNewSpec()
                .addNewAccessMode(pvc.getAccessModes())
                .withNewResources()
                .addToRequests(requests)
                .endResources()
                .withNewStorageClassName(nfsName)
                .endSpec()
                .build();
        try {
            PersistentVolumeClaim persistentVolumeClaim1 = client.persistentVolumeClaims().create(persistentVolumeClaim);
        }catch (Exception e){
            e.printStackTrace();
            return ResultVo.renderErr(CodeEnum.SERVER_ERR).withRemark("创建失败" + e.getMessage());
        }
        pvcDao.save(pvc);
        return ResultVo.renderOk().withRemark("创建持久卷成功！");
    }

    /**
     * 同步pvc信息核pv信息
     */
    @Override
    public void syncPvc(){
        List<PersistentVolumeClaim>  items = client.persistentVolumeClaims().list().getItems();
        List<Pvc> pvcList = pvcDao.findAll();
        for (Pvc pvc : pvcList){
            PersistentVolumeClaim item = client.persistentVolumeClaims().inNamespace(pvc.getNamespace()).withName(pvc.getPvcName()).get();
            if (null == item) {
                pvc.setStatus("missing");
                continue;
            }
            String creationTimestamp = item.getMetadata().getCreationTimestamp();
            String volumeName = item.getSpec().getVolumeName();
            String phase = item.getStatus().getPhase();
            if (null != volumeName){
                PersistentVolume persistentVolume = client.persistentVolumes().withName(volumeName).get();
                String reclaimPolicy = client.persistentVolumes().withName(volumeName).get().getSpec().getPersistentVolumeReclaimPolicy();
                pvc.setReclaimPolicy(reclaimPolicy);
            }
            Date date = Date.from(Instant.parse(creationTimestamp));
            pvc.setCreated(date.toLocaleString());
            pvc.setModified(new Date().toLocaleString());
            pvc.setPvname(volumeName);
            pvc.setStatus(phase);
        }
        pvcDao.saveAll(pvcList);
    }

    @Override
    public ResultVo deletePvc(String namespace, String pvcName) {
        if (namespace.equals("default") || namespace.equals("kube-public") || namespace.equals("kube-system")){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("系统名称空间存储卷禁止删除！");
        }
        client.persistentVolumeClaims().inNamespace(namespace).withName(pvcName).delete();
        Pvc byPvcName = pvcDao.findByPvcName(namespace, pvcName);
        pvcDao.delete(byPvcName);
        return ResultVo.renderOk().withRemark("删除任务已经提交，数据更新可能存在延迟");
    }

    /**
     *
     * @param namespace
     * @param pvcName
     * @param relativePath 相对路径 以/开头，例如: /linjb/tmp
     * @return
     */
    @Override
    public ResultVo findFilesInVolumes(String namespace, String pvcName, String relativePath,
                                       String sort, String order, String keyword) {
        Pvc pvc = pvcDao.findByPvcName(namespace, pvcName);
        if (pvc==null){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("存储卷不存在");
        }
        String pvname = pvc.getPvname();
        String server = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getServer();
        String path = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getPath();
        EcsResource ecsResource = ecsResourceDao.findByIp(server);
        if (ecsResource==null){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("请检查是否已经录入了nfs主机信息: "+ server);
        }
        List<LsEntryVo> files = new ArrayList<>();
        try{
            sftp = new Sftp(server, ecsResource.getRemotePort(),ecsResource.getAccount(), ecsResource.getPassword());
            List<ChannelSftp.LsEntry> tmpfiles = sftp.getClient().ls(path+relativePath);
            SftpATTRS lstat = sftp.getClient().lstat(path + relativePath);
            if (!lstat.isDir()){
                return ResultVo.renderErr(CodeEnum.ERR).withRemark("当前不是文件夹");
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            tmpfiles.forEach(lsEntry -> {
                LsEntryVo vo = new LsEntryVo();
                if(lsEntry.getFilename().equals(".") || lsEntry.getFilename().equals("..")) {

                }else {
                    String mtimeString = lsEntry.getAttrs().getMtimeString();
                    Date date = new Date(mtimeString);
                    vo.setDate(date.toLocaleString());
                    vo.setSize(lsEntry.getAttrs().getSize());
                    vo.setFilename(lsEntry.getFilename());
                    vo.setLongName(relativePath+lsEntry.getFilename());
                    vo.setFolder(lsEntry.getAttrs().isDir());
                    files.add(vo);
                }
            });
        }catch (Exception e){
            sftp.close();
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("查询文件列表出错！");
        }
        sftp.close();

        switch (sort){
            case "date":
                if (order.equals("asc")) {
                    files.sort((LsEntryVo vo1, LsEntryVo vo2) -> vo1.getDate().compareTo(vo2.getDate()));
                }else {
                    files.sort((LsEntryVo vo1, LsEntryVo vo2) -> vo2.getDate().compareTo(vo1.getDate()));
                }
                break;
            case "size":
                if (order.equals("asc")) {
                    files.sort((LsEntryVo vo1, LsEntryVo vo2) -> vo1.getSize().compareTo(vo2.getSize()));
                }else {
                    files.sort((LsEntryVo vo1, LsEntryVo vo2) -> vo2.getSize().compareTo(vo1.getSize()));
                }
                break;
            case "name":
                if (order.equals("asc")) {
                    files.sort((LsEntryVo vo1, LsEntryVo vo2) -> vo1.getFilename().compareTo(vo2.getFilename()));
                }else {
                    files.sort((LsEntryVo vo1, LsEntryVo vo2) -> vo2.getFilename().compareTo(vo1.getFilename()));
                }
                break;
            default:
                break;
        }

        if (Strings.isNotEmpty(keyword)){
            Iterator<LsEntryVo> iterator = files.iterator();
            while (iterator.hasNext()){
                LsEntryVo next = iterator.next();
                if (!next.getFilename().contains(keyword)) iterator.remove();
            }
        }
        return ResultVo.renderOk(files).withRemark("查询成功！");
    }



    /**
     * 遍历文件夹 找出所有文件-此方法未完成，会卡死--不允许使用
     * @deprecated
     * @param dir
     */
    public List<LsEntryVo> listFiles(String dir) throws SftpException {
        List<ChannelSftp.LsEntry> tmpfiles = this.getSftp().getClient().ls(dir);
        List<LsEntryVo> vos = new ArrayList<>();
        for (int i=0; i<tmpfiles.size(); i++){
            LsEntryVo vo = new LsEntryVo();
            ChannelSftp.LsEntry lsEntry = tmpfiles.get(i);
            vo.setFilename(lsEntry.getFilename());
            vo.setLongName(lsEntry.getLongname());
            vo.setSize(lsEntry.getAttrs().getSize());
            vo.setFolder(lsEntry.getAttrs().isDir());
            if (vo.isFolder()){
                List<LsEntryVo> lsEntryVos = listFiles(vo.getFilename());
//                vo.setSubFiles(lsEntryVos);
            }
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public ResultVo uploadFile(String namespace, String pvcName,String realPath, MultipartFile file) {
        Pvc pvc = pvcDao.findByPvcName(namespace, pvcName);
        if (pvc==null){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("存储卷不存在");
        }
        String pvname = pvc.getPvname();
        String server = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getServer();
        String path = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getPath()+"/"+realPath+"/";
        System.out.println("path = " + path);
        EcsResource ecsResource = ecsResourceDao.findByIp(server);
        if (ecsResource==null){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("请检查是否已经录入了nfs主机信息: "+ server);
        }
        try {
            InputStream inputStream = file.getInputStream();
            String fileName = file.getOriginalFilename();
            System.out.println("fileName = " + fileName);
            sftp = new Sftp(server, ecsResource.getRemotePort(),ecsResource.getAccount(), ecsResource.getPassword());
            sftp.getClient().put(inputStream,path+fileName, new FileProgressMonitor(fileName, file.getSize()));
        }catch (Exception e){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("上传文件出错！");
        }
        sftp.close();

        return ResultVo.renderOk().withRemark("上传文件成功！");
    }

    /**
     * 初始化sftp
     */
    public Sftp getSftp(){
        if (sftp!=null) return sftp;
        String server = client.apps().deployments().inNamespace("default").withName("nfs-client-provisioner").get().getSpec()
                .getTemplate().getSpec().getVolumes().get(0).getNfs().getServer();
        EcsResource byIp = ecsResourceDao.findByIp(server);
        if (null == byIp){
            LOG.info("请录入nfs服务器信息" + server);
            return null;
        }
        sftp = new Sftp(byIp.getIp(),byIp.getRemotePort(), byIp.getAccount(), byIp.getPassword());
        return sftp;
    }

    @Override
    public ResultVo getStoragerInfo() {
//        String server = client.apps().deployments().inNamespace("default").withName("nfs-client-provisioner").get().getSpec()
//                .getTemplate().getSpec().getVolumes().get(0).getNfs().getServer();
        //获取实际运行nfs服务的机器ip
        String hostIP = client.pods().inNamespace("default").withLabelIn("app", "nfs-client-provisioner")
                .list().getItems().get(0).getStatus().getHostIP();

        String path = client.apps().deployments().inNamespace("default").withName("nfs-client-provisioner").get().getSpec()
                .getTemplate().getSpec().getVolumes().get(0).getNfs().getPath();

        EcsResource byIp = ecsResourceDao.findByIp(hostIP);
        if (null == byIp){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("请录入nfs服务器信息" + hostIP);
        }
        Session session = JschUtil.getSession(byIp.getIp(), byIp.getRemotePort(), byIp.getAccount(), byIp.getPassword());
        String size = JschUtil.exec(session, "df -h | grep " + path + " | head -1 | awk '{print $2}'", Charset.forName("UTF-8"));
        String used = JschUtil.exec(session, "df -h | grep " + path + " | head -1 | awk '{print $3}'", Charset.forName("UTF-8"));
        String avail = JschUtil.exec(session, "df -h | grep " + path + " | head -1 | awk '{print $4}'", Charset.forName("UTF-8"));

        Map map = new HashMap();
        map.put("size", size.replace("G","").trim());
        map.put("used", used.replace("G","").trim());
        map.put("avail", avail.replace("G","").trim());

        return ResultVo.renderOk(map).withRemark("查询存储成功！");
    }

    @Override
    public long findCount() {
       return pvcDao.count();
    }

    @Override
    public ResultVo deleteFiles(String namespace, String pvcName, List<String> filePaths) {
        Pvc pvc = pvcDao.findByPvcName(namespace, pvcName);
        if (pvc==null){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("存储卷不存在");
        }
        String pvname = pvc.getPvname();
        String server = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getServer();
        String path = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getPath();
        EcsResource ecsResource = ecsResourceDao.findByIp(server);
        if (ecsResource==null){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("请检查是否已经录入了nfs主机信息: "+ server);
        }
        sftp = new Sftp(server, ecsResource.getRemotePort(),ecsResource.getAccount(), ecsResource.getPassword());
        if (path==null || path.equals("/") || path.equals("")){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("删除异常");
        }
        for (int i=0; i< filePaths.size(); i++){
            String filePath = filePaths.get(i);
            if (!filePath.startsWith("/")){
                return ResultVo.renderOk(CodeEnum.ERR).withRemark("文件路径非法");
            }
            if (filePath.endsWith("/")){
                sftp.delDir(path+filePath);
            }else {
                sftp.delFile(path+filePath);
            }
        }
        return ResultVo.renderOk().withRemark("删除文件");
    }

    @Override
    public ResultVo newFolder(String namespace, String pvcName, String relativePath, String folderName) {
        Pvc pvc = pvcDao.findByPvcName(namespace, pvcName);
        if (pvc==null){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("存储卷不存在");
        }
        String pvname = pvc.getPvname();
        String server = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getServer();
        String path = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getPath();
        EcsResource ecsResource = ecsResourceDao.findByIp(server);
        if (ecsResource==null){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("请检查是否已经录入了nfs主机信息: "+ server);
        }
        sftp = new Sftp(server, ecsResource.getRemotePort(),ecsResource.getAccount(), ecsResource.getPassword());
        boolean exist = sftp.exist(path + relativePath + folderName);
        if (exist){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("文件夹已经存在");
        }
        boolean mkdir = sftp.mkdir(path+relativePath + folderName);
        if (mkdir){
            return ResultVo.renderOk(CodeEnum.OK).withRemark("创建文件夹成功!" + relativePath+folderName);
        }
        return ResultVo.renderOk(CodeEnum.ERR).withRemark("创建失败" + relativePath+ folderName);
    }

    @Override
    public InputStream download(String namespace, String pvcName,String filePath, String fileName) throws SftpException, IOException {
        Pvc pvc = pvcDao.findByPvcName(namespace, pvcName);
        if (pvc==null){
            return null;
        }
        String pvname = pvc.getPvname();
        String server = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getServer();
        String path = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getPath();
        EcsResource ecsResource = ecsResourceDao.findByIp(server);
        if (ecsResource==null){
            return null;
        }
        sftp = new Sftp(server, ecsResource.getRemotePort(),ecsResource.getAccount(), ecsResource.getPassword());
        SftpATTRS lstat = sftp.getClient().lstat(path + filePath);
        InputStream inputStream;
        if (lstat.isDir()){
            LOG.info("文件夹打包下载");
//            File tmpFile = File.createTempFile("temp",".tmp");
//            LOG.info("打包的文件地址：" + tmpFile.getAbsolutePath());
//            sftp.recursiveDownloadFolder(path+fileName, tmpFile);
//            inputStream = new FileInputStream(tmpFile);
            inputStream = sftp.getClient().get(path+filePath);
        }else {
            LOG.info("单文件下载");
            inputStream = sftp.getClient().get(path+filePath);
        }
        return inputStream;
    }

    @Override
    public ResultVo findPvcDetail(String namespace, String pvcName) {
        Pvc byPvcName = pvcDao.findByPvcName(namespace, pvcName);
        if (null != byPvcName){
            return ResultVo.renderOk(byPvcName).withRemark("查询存储卷详情成功，数据来自数据库");
        }
        syncPvc();
        return ResultVo.renderErr(CodeEnum.ERR).withRemark("存储卷不存在，可能是由于信息未同步导致！，请重新尝试");
    }

    @Override
    public List<Pvc> find(Map<String, Object> params, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize,Sort.by("created").descending());
        Specification<Pvc> specification = new Specification<Pvc>() {
            @Override
            public Predicate toPredicate(Root<Pvc> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> {
                    list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%"));
                });
                return criteriaQuery.where(list.toArray(new Predicate[list.size()])).getRestriction();
            }
        };

        Page<Pvc> pageObject = pvcDao.findAll(specification, pageable);
        List<Pvc> pvcList = new ArrayList<>();
        pageObject.getContent().forEach(tmp -> {
            pvcList.add(tmp);
        });
        return pvcList;
    }

    @Override
    public long count(Map<String, Object> params) {
        Specification<Pvc> specification = new Specification<Pvc>() {
            @Override
            public Predicate toPredicate(Root<Pvc> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> {
                    list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%"));
                });
                return criteriaQuery.where(list.toArray(new Predicate[list.size()])).getRestriction();
            }
        };
        return pvcDao.count(specification);
    }


    /**
     * 搜索过滤文件
     * @param channelSftp
     * @param path
     * @return
     * @throws SftpException
     */
    private List<LsEntryVo> listEntries(final ChannelSftp channelSftp, final String path, final String file) throws SftpException {
        List<LsEntryVo> files = new ArrayList<>();

        final Vector<ChannelSftp.LsEntry> vector = new Vector<ChannelSftp.LsEntry>();

        ChannelSftp.LsEntrySelector selector = new ChannelSftp.LsEntrySelector() {
            public int select(ChannelSftp.LsEntry entry)  {
                LsEntryVo vo = new LsEntryVo();
                final String filename = entry.getFilename();
                vo.setFilename(filename);
                vo.setSize(entry.getAttrs().getSize());
//                vo.setLongName();
//                vo.setDate();
                if (filename.equals(".") || filename.equals("..")) {
                    return CONTINUE;
                }
                if (entry.getAttrs().isLink()) {
                    vector.addElement(entry);
                    files.add(vo);
                }
                else if (entry.getAttrs().isDir()) {
                    vo.setFolder(true);
                    files.add(vo);
                }
                else {
                   files.add(vo);
                }
                return CONTINUE;
            }
        };

        channelSftp.ls(path, selector);

        return files;
    }
}
