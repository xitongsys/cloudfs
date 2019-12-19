package cloud.utils;

import com.google.gson.annotations.Since;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Path {
    public static String concat(String pa, String pb){
        if(pa==null && pb==null) return "";
        if(pa==null || pa.length() <= 0) return pb;
        if(pb==null || pb.length() <= 0) return pa;

        int la = pa.length(), lb = pb.length();
        if(pa.charAt(la - 1) == '/'){
            pa = pa.substring(0, la-1);
        }

        if(pb.charAt(0) == '/'){
            pb = pb.substring(1);
        }

        return pa + "/" + pb;
    }

    public static String concat(String[] ps){
        String res = "";
        for(String p : ps){
            res = concat(res, p);
        }
        return res;
    }

    public static String reduce(String p){
        String[] cps = p.split("/");
        List<String> res = new ArrayList<String>();
        if(p.charAt(0) == '/'){
            res.add("");//root
        }

        for(String cp : cps){
            if(cp.length() == 0) continue;
            if(cp.equals("..") && res.size()>0){
                int ln = res.size();
                if(res.get(ln-1).equals("..")){
                    res.add(cp);
                }else if(res.get(ln-1) != ""){
                    res.remove(res.size()-1);
                }

            }else if(!cp.equals(".")){
                res.add(cp);
            }
        }

        return String.join("/", res);
    }

    public static String parent(String pa){
        int la = pa.length();
        if(la <= 1) return pa;
        int i = la - 1;
        if(pa.charAt(la - 1) == '/'){
            pa = pa.substring(0, la - 1);
            i--;
        }

        while(i>=0 && pa.charAt(i)!='/'){
            i--;
        }

        return pa.substring(0, i);
    }

    public static String basename(String pa){
        int la = pa.length();
        int i = la -1;
        while(i>=0 && pa.charAt(i) != '/')i--;
        return pa.substring(i+1, la);
    }

    public static boolean exists(String path){
        File f = new File(path);
        return f.exists();
    }

    public static void mkdir(String path){
        File f = new File(path);
        f.mkdir();
    }

}
