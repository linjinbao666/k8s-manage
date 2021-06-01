package ecs.service;

import com.jcraft.jsch.SftpException;
import ecs.entity.Pvc;
import ecs.vo.ResultVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

public interface PvcService {
    List<Pvc> findAll();

    ResultVo createPvc(Pvc pvc);

    List<Pvc> find(Map<String, Object> params, Integer pageNum, Integer pageSize);

    long count(Map<String, Object> params);

    void syncPvc();

    ResultVo deletePvc(String pvcName, String namespace);


    ResultVo findFilesInVolumes(String namespace, String pvcName, String relativePath,
                                String sort, String order, String keyword);

    ResultVo uploadFile(String namespace, String pvcName,String realPath, MultipartFile file);

    ResultVo getStoragerInfo();

    long findCount();

    ResultVo deleteFiles(String namespace, String pvcName, List<String> filePaths);

    ResultVo newFolder(String namespace, String pvcName, String relativePath, String folderName);

    InputStream download(String namespace, String pvcName, String filePath, String fileName) throws SftpException, IOException;

    ResultVo findPvcDetail(String namespace, String pvcName);
}
