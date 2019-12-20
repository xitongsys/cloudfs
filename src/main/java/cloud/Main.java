package cloud;

import cloud.adl.FileSystemADL;
import cloud.utils.Config;
import cloud.utils.FileEntry;
import cloud.utils.FileSystem;
import cloud.utils.FormatterOutput;
import cloud.webhdfs.FileSystemWebHDFS;

import java.util.List;


public class Main {
    public static void main(String[] args) throws Exception{
        String confPath = System.getenv("CLOUDFSCONFIG");
        Config config = new Config();
        config.importConfig(confPath);
        FileSystemADL adl = new FileSystemADL(config.adl);
        FileSystemWebHDFS webhdfs = new FileSystemWebHDFS(config.webhdfs);
        FileSystem fs = null;

        try {
            int na = args.length;
            String target = args[0];
            String cmd = args[1];
            
            if(target.equals("adl")){
                fs = adl;
            }else if(target.equals("webhdfs")){
                fs = webhdfs;
            }

            if (cmd.equals("cd")) {
                fs.cd(args[2]);

            }else if(cmd.equals("info")){
                System.out.println(fs.info());

            } else if (cmd.equals("ls")) {
                String path = na > 2 ? args[2] : "";
                List<FileEntry> ds = fs.ls(path);
                FormatterOutput.ls(ds);

            } else if (cmd.equals("cat")) {
                List<Byte> buf = fs.cat(args[2]);
                for(Byte c : buf){
                    System.out.print((char)c.intValue());
                }

            } else if (cmd.equals("chmod")) {
                fs.chmod(args[3], args[2]);

            } else if (cmd.equals("rm")) {
                fs.rm(args[2]);

            } else if (cmd.equals("mv")) {
                fs.mv(args[2], args[3]);

            } else if (cmd.equals("mkdir")) {
                fs.mkdir(args[2]);

            } else if (cmd.equals("put")) {
                fs.put(args[3], args[2]);

            } else if (cmd.equals("putall")){
                fs.putall(args[3], args[2]);

            } else if (cmd.equals("get")) {
                fs.get(args[2], args[3]);

            } else if (cmd.equals("getall")){
                fs.getall(args[2], args[3]);

            }else if (cmd.equals("pwd")){
                System.out.println(fs.pwd());

            } else {
                throw new IllegalArgumentException("Unsupported operator: " + cmd);
            }
        }catch (Exception e){
            System.out.println(e);
        }

        config.exportConfig(confPath);
    }
}
