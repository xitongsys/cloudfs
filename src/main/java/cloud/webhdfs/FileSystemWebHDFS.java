package cloud.webhdfs;

import cloud.utils.*;
import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;

class FileStatus {
    public Long modificationTime;
    public String type;
    public Long length;
    public String permission;
    public String owner;
    public String pathSuffix;

    public FileEntry toFileEntry(){
        FileEntry entry = new FileEntry();
        entry.name = pathSuffix;
        entry.length = length;
        if(type.equals("FILE")) entry.type = FileEntry.FILE;
        else entry.type = FileEntry.DIR;
        entry.permission = permission;
        entry.owner = owner;
        entry.lastModifiedTime = new Date(modificationTime);

        return entry;
    }
}

class FileStatuses {
    public FileStatus[] FileStatus;
}

class LsResponse {
    public FileStatuses FileStatuses;
}

class GetStatusResponse {
    public FileStatus FileStatus;
}

public class FileSystemWebHDFS implements FileSystem {
    private static final String HDFSPREFIX = "http://";
    private static final String HDFSSUFFIX = "webhdfs/v1";

    private ConfigWebHDFS config = null;
    public FileSystemWebHDFS(ConfigWebHDFS config){
        this.config = config;
    }

    @Override
    public void cd(String path) throws Exception {
        if(path == null || path.length() <= 0) return;

        if (path.startsWith(HDFSPREFIX)) {
            URI uri = new URI(path);
            config.server = uri.getHost();
            config.currentDirectory = uri.getPath();
            return;
        }

        if(path.charAt(0)=='/'){
            config.currentDirectory = path;
        }else{
            config.currentDirectory = Path.reduce(Path.concat(config.currentDirectory, path));
        }
    }

    @Override
    public String pwd() throws Exception {
        return config.currentDirectory;
    }

    @Override
    public String info() throws Exception {
        Gson gson = new Gson();
        return gson.toJson(config);
    }

    private void _mv(String p0, String p1) throws Exception {
        String fp0 = getFullPath(p0), ap1 = getAbsolutePath(p1);
        URL url = new URL(fp0 + "?op=RENAME&destination=" + ap1 + "&user.name=" + config.user);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setInstanceFollowRedirects(true);

        InputStream in = con.getInputStream();
        String content = Util.bytes2str(Util.readAll(in));
        in.close();
        con.disconnect();
    }
    @Override
    public void mv(String p0, String p1) throws Exception {
        _mv(p0, p1);
    }

    private FileEntry _getstatus(String path) throws Exception {
        URL url = new URL(path + "?op=GETFILESTATUS&user.name=" + config.user);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(true);

        InputStream in = con.getInputStream();
        String content = Util.bytes2str(Util.readAll(in));
        in.close();
        con.disconnect();

        Gson gson = new Gson();
        GetStatusResponse getStatusResponse = gson.fromJson(content, GetStatusResponse.class);
        return getStatusResponse.FileStatus.toFileEntry();
    }

    private boolean _isExist(String path) throws Exception {
        try {
            _getstatus(path);
        }catch (FileNotFoundException e){
            return false;
        }

        return true;
    }

    private List<Byte> _cat(String path) throws Exception {
        URL url = new URL(path + "?op=OPEN&user.name=" + config.user);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(true);

        InputStream in = con.getInputStream();
        List<Byte> content = Util.readAll(in);
        in.close();
        con.disconnect();
        return content;
    }
    @Override
    public List<Byte> cat(String path) throws Exception {
        return _cat(getFullPath(path));
    }

    //path is full path
    private List<FileEntry> _ls(String path) throws Exception {
        URL url = new URL(path + "?op=LISTSTATUS&user.name=" + config.user);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(true);

        InputStream in = con.getInputStream();
        String content = Util.bytes2str(Util.readAll(in));
        in.close();
        con.disconnect();

        Gson gson = new Gson();
        LsResponse lsResponse = gson.fromJson(content, LsResponse.class);
        List<FileEntry> res = new ArrayList<>();
        for(FileStatus sta : lsResponse.FileStatuses.FileStatus){
            res.add(sta.toFileEntry());
        }

        return res;
    }

    @Override
    public List<FileEntry> ls(String path) throws Exception {
        return _ls(getFullPath(path));
    }

    public void _rm(String path) throws Exception {
        URL url = new URL(path + "?op=DELETE&recursive=true&user.name=" + config.user);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("DELETE");
        con.setInstanceFollowRedirects(true);

        InputStream in = con.getInputStream();
        String content = Util.bytes2str(Util.readAll(in));
        in.close();
        con.disconnect();
    }
    @Override
    public void rm(String path) throws Exception {
        _rm(getFullPath(path));
    }

    private void _chmod(String path, String mod) throws Exception {
        URL url = new URL(path + "?op=SETPERMISSION&permission=" + mod + "&user.name=" + config.user);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setInstanceFollowRedirects(true);

        InputStream in = con.getInputStream();
        String content = Util.bytes2str(Util.readAll(in));
        in.close();
        con.disconnect();
    }
    @Override
    public void chmod(String path, String mod) throws Exception {
        _chmod(getFullPath(path), mod);
    }

    private void _put(String path, String localPath) throws Exception {
        URL url = new URL(path + "?op=CREATE&overwrite=true&replication=2&user.name=" + config.user);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setInstanceFollowRedirects(false);
        con.connect();
        String redirectUrl = con.getHeaderField("Location");

        if(redirectUrl != null){
            url = new URL(redirectUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("PUT");
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Content-Type", "application/octet-stream");

            InputStream input = new FileInputStream(localPath);
            final int _SIZE = input.available();
            con.setFixedLengthStreamingMode(_SIZE);
            con.connect();
            OutputStream output = con.getOutputStream();
            while(true){
                int c = input.read();
                if(c < 0) break;
                output.write(c);
            }
            output.close();

            InputStream in = con.getInputStream();
            String content = Util.bytes2str(Util.readAll(in));
            in.close();
            con.disconnect();

        }
    }
    @Override
    public void put(String path, String localPath) throws Exception {
        _put(getFullPath(path), localPath);
    }

    private void _mkdir(String path) throws Exception {
        URL url = new URL(path + "?op=MKDIRS&user.name=" + config.user);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setInstanceFollowRedirects(true);

        InputStream in = con.getInputStream();
        String content = Util.bytes2str(Util.readAll(in));
        in.close();
        con.disconnect();
    }
    @Override
    public void mkdir(String path) throws Exception {
        _mkdir(getFullPath(path));
    }

    private void _get(String path, String localPath) throws Exception {
        URL url = new URL(path + "?op=OPEN&user.name=" + config.user);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(true);

        InputStream in = con.getInputStream();
        if(localPath == null || localPath.length()==0 || localPath.equals(".") || localPath.endsWith("/.") || localPath.endsWith("/")){
            localPath = Path.reduce(Path.concat(localPath, Path.basename(path)));
        }
        OutputStream output = new BufferedOutputStream(new FileOutputStream(localPath));
        InputStream input = new BufferedInputStream(in);

        while(true){
            int c = input.read();
            if(c < 0) break;
            output.write(c);
        }

        output.close();
        in.close();
        con.disconnect();
    }
    @Override
    public void get(String path, String localPath) throws Exception {
        _get(getFullPath(path), localPath);
    }

    private void _getall(String path, String localPath) throws Exception{
        Stack<String[]> sk = new Stack<String[]>();
        sk.push(new String[]{path, localPath});
        while(!sk.empty()){
            String[] cur = sk.pop();
            String p = cur[0], lp = cur[1];
            FileEntry en = _getstatus(p);
            if(en.type.equals(FileEntry.DIR)){
                if(!Path.exists(lp)){
                    Path.mkdir(lp);
                }

                for(FileEntry d : _ls(p)){
                    String name = d.name;
                    sk.push(new String[]{Path.concat(p, name), Path.concat(lp, name)});
                }

            }else if (en.type.equals(FileEntry.FILE)){
                _get(p, lp);
            }
        }
    }
    @Override
    public void getall(String path, String localPath) throws Exception {
        _getall(getFullPath(path), localPath);
    }

    private void _putall(String path, String localPath) throws Exception {
        Stack<String[]> sk = new Stack<String[]>();
        sk.push(new String[]{path, localPath});
        while(!sk.empty()){
            String[] cur = sk.pop();
            String p = cur[0], lp = cur[1];
            File pf = new File(lp);
            if(pf.isDirectory()){
                if(!_isExist(p)){
                    _mkdir(p);
                }

                for(String name : pf.list()){
                    sk.push(new String[]{Path.concat(p, name), Path.concat(lp, name)});
                }

            }else {
                _put(p, lp);
            }
        }
    }
    @Override
    public void putall(String path, String localPath) throws Exception {
        _putall(getFullPath(path), localPath);
    }

    private String getAbsolutePath(String path) throws Exception {
        if(path.length() > 0 && path.charAt(0) == '/') return path;
        return Path.reduce(Path.concat(config.currentDirectory, path));
    }

    private String getFullPath(String path) throws Exception {
        if(path.startsWith(HDFSPREFIX)){
            return path;
        }

        return Path.concat(new String[]{HDFSPREFIX, config.server, HDFSSUFFIX, Path.reduce(getAbsolutePath(path))});
    }
}
