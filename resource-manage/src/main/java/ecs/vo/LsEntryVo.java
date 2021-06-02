package ecs.vo;

import java.io.Serializable;
import java.util.List;

public class LsEntryVo implements Serializable {

    private static final long serialVersionUID = 1490405057353441804L;
    private String filename;
    private boolean isFolder;
    private String longName;
    private String date;
    private Long size;


    public String getLongName() {
        return longName;
    }
    public void setLongName(String longName) {
        this.longName = longName;
    }
    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public boolean isFolder() {
        return isFolder;
    }
    public void setFolder(boolean isFolder) {
        this.isFolder = isFolder;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
