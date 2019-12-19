package cloud.utils;

import java.util.Date;

public class FileEntry {
    public static String DIR = "DIR";
    public static String FILE = "FILE";

    public String name;
    public String type;
    public String permission;
    public long length;
    public String owner;
    public Date lastModifiedTime;
    public String path;

    public FileEntry(){}
}

