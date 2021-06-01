package ecs.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * linux磁盘vo类
 */
public class DiskVo implements Serializable {
    private String fileSystem;
    private String size;
    private String used;
    private String avail;
    private String usePercent;

    public String getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(String fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getUsed() {
        return used;
    }

    public void setUsed(String used) {
        this.used = used;
    }

    public String getAvail() {
        return avail;
    }

    public void setAvail(String avail) {
        this.avail = avail;
    }

    public String getUsePercent() {
        return usePercent;
    }

    public void setUsePercent(String usePercent) {
        this.usePercent = usePercent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiskVo vo = (DiskVo) o;
        return Objects.equals(fileSystem, vo.fileSystem);
    }

}
