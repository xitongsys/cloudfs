package cloud.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Util {
    public static List<Byte> readAll(InputStream input) throws Exception{
        List<Byte> bf = new ArrayList<>();
        while(true){
            int c = input.read();
            if(c <=0 ) break;
            bf.add((byte)c);
        }

        return bf;
    }

    public static String bytes2str(List<Byte> bs){
        StringBuffer sb = new StringBuffer();
        for(byte b : bs){
            sb.append((char)b);
        }
        return sb.toString();
    }

    public static String size2human(long size){
        long B = 1L;
        long KB = 1024L * B;
        long MB = 1024L * KB;
        long GB = 1024L * MB;
        long TB = 1024L * GB;

        String fmt = "%.2f ";
        if(size < KB) return String.format(fmt + "B", (float)size / (float)B);
        else if(size < MB) return String.format(fmt + "KB", (float)size / (float)KB);
        else if(size < GB) return String.format(fmt + "MB", (float)size / (float)MB);
        else if(size < TB) return String.format(fmt + "GB", (float)size / (float)GB);
        else return String.format(fmt + "TB", (float)size / (float)TB);
    }
}
