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
}
