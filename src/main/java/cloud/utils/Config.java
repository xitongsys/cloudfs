package cloud.utils;

import cloud.adl.ConfigADL;
import com.google.gson.Gson;
import cloud.webhdfs.ConfigWebHDFS;

import java.io.*;

public class Config {
    public ConfigADL adl;
    public ConfigWebHDFS webhdfs;

    public Config(){}

    public void importConfig(String path) throws Exception {
        StringBuffer sb = new StringBuffer();
        InputStream input = new BufferedInputStream(new FileInputStream(path));
        while(true){
            int c = input.read();
            if(c < 0) break;
            sb.append((char)c);
        }
        input.close();
        String confJson = sb.toString();
        Gson gson = new Gson();
        Config conf = gson.fromJson(confJson, Config.class);

        this.adl = conf.adl;
        this.webhdfs = conf.webhdfs;
    }

    public void exportConfig(String path) throws Exception {
        OutputStream output = new BufferedOutputStream(new FileOutputStream(path));
        Gson gson = new Gson();
        String confJson = gson.toJson(this);
        output.write(confJson.getBytes());
        output.close();
    }
}
