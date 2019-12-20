package cloud.utils;

import java.util.List;

public interface FileSystem {
    void cd(String path) throws Exception;
    String pwd() throws Exception;
    String info() throws Exception;
    void mv(String p0, String p1) throws Exception;
    List<Byte> cat(String path) throws Exception;
    List<FileEntry> ls(String path) throws Exception;
    void rm(String path) throws Exception;
    void chmod(String path, String mod) throws Exception;
    void put(String path, String localPath) throws Exception;
    void mkdir(String path) throws Exception;
    void get(String path, String localPath) throws Exception;
    void getall(String path, String localPath) throws Exception;
    void putall(String path, String localPath) throws Exception;
}
