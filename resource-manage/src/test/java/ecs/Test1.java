package ecs;

import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.*;
import ecs.vo.LsEntryVo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class Test1 {

    static ChannelSftp channelSftp = null;
    static Session session = null;
    static Channel channel = null;
    static String PATHSEPARATOR = "/";

    public static void main(String[] args) {
        String SFTPHOST = "*.16.12.22"; // SFTP Host Name or SFTP Host IP Address
        int SFTPPORT = 22; // SFTP Port Number
        String SFTPUSER = "root"; // User Name
        String SFTPPASS = "flineCloud@2020root"; // Password

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
            session.setPassword(SFTPPASS);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();


//            channelSftp = sftp.getClient();

            List<LsEntryVo> vos = listEntries(channelSftp, path, "学习");
            Iterator<LsEntryVo> iterator = vos.iterator();
            while (iterator.hasNext()){
                LsEntryVo next = iterator.next();
                System.out.println(next.getFilename());
            }
            System.out.println(vos);

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (channelSftp != null)
                channelSftp.disconnect();
            if (channel != null)
                channel.disconnect();
            if (session != null)
                session.disconnect();

        }

    }

//    private static Sftp sftp = new Sftp("*.16.12.22",22,"root", "flineCloud@2020root");
    private static String path = "/data/dyn";


    @SuppressWarnings("unchecked")
    private static void recursiveFolderDownload(String sourcePath, String destinationPath) throws SftpException {
        Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(sourcePath); // Let list of folder content

        //Iterate through list of folder content
        for (ChannelSftp.LsEntry item : fileAndFolderList) {

            if (!item.getAttrs().isDir()) { // Check if it is a file (not a directory).
                if (!(new File(destinationPath + PATHSEPARATOR + item.getFilename())).exists()
                        || (item.getAttrs().getMTime() > Long
                        .valueOf(new File(destinationPath + PATHSEPARATOR + item.getFilename()).lastModified()
                                / (long) 1000)
                        .intValue())) { // Download only if changed later.

                    new File(destinationPath + PATHSEPARATOR + item.getFilename());
                    channelSftp.get(sourcePath + PATHSEPARATOR + item.getFilename(),
                            destinationPath + PATHSEPARATOR + item.getFilename()); // Download file from source (source filename, destination filename).
                }
            } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
                new File(destinationPath + PATHSEPARATOR + item.getFilename()).mkdirs(); // Empty folder copy.
                recursiveFolderDownload(sourcePath + PATHSEPARATOR + item.getFilename(),
                        destinationPath + PATHSEPARATOR + item.getFilename()); // Enter found folder on server to read its contents and create locally.
            }
        }
    }

    private static List<LsEntryVo> listEntries(final ChannelSftp channelSftp, final String path, final String file) throws SftpException {
        List<LsEntryVo> files = new ArrayList<>();

        final Vector<ChannelSftp.LsEntry> vector = new Vector<ChannelSftp.LsEntry>();

        ChannelSftp.LsEntrySelector selector = new ChannelSftp.LsEntrySelector() {
            public int select(ChannelSftp.LsEntry entry)  {
                LsEntryVo vo = new LsEntryVo();
                final String filename = entry.getFilename();
                vo.setFilename(filename);
                vo.setSize(entry.getAttrs().getSize());
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
