package cloud.utils;

import com.microsoft.azure.datalake.store.DirectoryEntry;

import java.util.List;

public class FormatterOutput {
    public static void ls(List<FileEntry> ds){
        String format = "%s\t%s\t%s\t%s\t%s\t%s\n";
        System.out.format(format, "Mode", "Owner", "Type", "Size", "LastModifiedTime", "Name");
        System.out.println("");
        for(FileEntry d : ds){
            System.out.format(format, d.permission, d.owner, d.type, d.length, d.lastModifiedTime, d.name);
        }
    }

}
