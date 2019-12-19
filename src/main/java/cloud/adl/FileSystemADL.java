package cloud.adl;

import java.io.*;
import java.net.URI;
import java.util.*;

import cloud.utils.FileEntry;
import cloud.utils.FileSystem;
import cloud.utils.Path;
import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.datalake.store.DirectoryEntry;
import com.microsoft.azure.datalake.store.DirectoryEntryType;
import com.microsoft.azure.datalake.store.IfExists;
import com.microsoft.azure.datalake.store.oauth2.ClientCredsTokenProvider;

public class FileSystemADL
implements FileSystem {
    private static final String ADLPREFIX = "adl://";
    ConfigADL config = null;
    private ClientCredsTokenProvider provider;
    private ADLStoreClient client;

    public FileSystemADL(ConfigADL config) {
        this.config = config;
        this.provider = new ClientCredsTokenProvider(config.endpoint, config.clientId, config.clientKey);
    }

    @Override
    public void cd(String path) throws Exception {
        if(!isExist(path)){
            throw new Exception("path not found");
        }

        if (path.startsWith(ADLPREFIX)) {
            URI uri = new URI(path);
            String pathAccount = uri.getHost();
            if (!pathAccount.equals(config.account) || client == null) {
                config.account = pathAccount;
                client = ADLStoreClient.createClient(config.account, provider);
            }
            config.currentDirectory =  uri.getPath();

        } else if(path.length() >= 1 && path.charAt(0)=='/') {
            config.currentDirectory = path;

        } else {
            config.currentDirectory = Path.reduce(Path.concat(config.currentDirectory, path));
        }
    }

    @Override
    public String pwd() throws Exception {
        return ADLPREFIX + Path.concat(config.account, config.currentDirectory);
    }

    private void _mv(String p0, String p1) throws Exception {
        client.rename(p0, p1);
    }

    @Override
    public void mv(String p0, String p1) throws Exception {
        p0 = init(p0);
        p1 = init(p1);
        _mv(p0, p1);
    }

    private boolean _isExist(String path) throws Exception {
        return client.checkExists(path);
    }

    public boolean isExist(String path) throws Exception {
        path = init(path);
        return _isExist(path);
    }

    private List<Byte> _cat(String path) throws Exception {
        InputStream input = new BufferedInputStream(client.getReadStream(path));
        List<Byte> buf = new ArrayList<Byte>();
        while(true){
            int c = input.read();
            if(c < 0) break;
            buf.add((byte)c);
        }

        return buf;
    }

    @Override
    public List<Byte> cat(String path) throws Exception {
        path = init(path);
        return _cat(path);
    }

    private FileEntry directoryEntry2fileEntry(DirectoryEntry entry){
        FileEntry fileEntry = new FileEntry();
        fileEntry.name = entry.name;
        fileEntry.lastModifiedTime = entry.lastModifiedTime;
        fileEntry.length = entry.length;
        fileEntry.owner = entry.user;
        fileEntry.permission = entry.permission;
        if(entry.type == DirectoryEntryType.FILE) fileEntry.type = FileEntry.FILE;
        else fileEntry.type = FileEntry.DIR;
        fileEntry.path = entry.fullName;

        return fileEntry;
    }

    private List<FileEntry> _ls(String path) throws Exception {
        List<FileEntry> list = new ArrayList<>();
        for(DirectoryEntry entry : client.enumerateDirectory(path)){
            list.add(directoryEntry2fileEntry(entry));
        }
        return list;
    }

    @Override
    public List<FileEntry> ls(String path) throws Exception {
        path = init(path);
        return _ls(path);
    }

    private void _rm(String path) throws Exception {
        client.deleteRecursive(path);
    }

    @Override
    public void rm(String path) throws Exception {
        path = init(path);
        _rm(path);
    }

    private void _chmod(String path, String mod) throws Exception {
        client.setPermission(path, mod);
    }

    @Override
    public void chmod(String path, String mod) throws Exception {
        path = init(path);
        _chmod(path, mod);
    }

    private void _put(String path, String localPath) throws Exception {
        OutputStream output = new BufferedOutputStream(client.createFile(path, IfExists.OVERWRITE));
        InputStream input = new BufferedInputStream(new FileInputStream(localPath));

        while(true){
            int c = input.read();
            if(c < 0) break;
            output.write(c);
        }

        output.close();
    }

    @Override
    public void put(String path, String localPath) throws Exception {
        path = init(path);
        _put(path, localPath);
    }

    private void _mkdir(String path) throws Exception {
        client.createDirectory(path);
    }

    @Override
    public void mkdir(String path) throws Exception {
        path = init(path);
        _mkdir(path);
    }

    private void _get(String path, String localPath) throws Exception{
        if(localPath == null || localPath.length()==0 || localPath.equals(".") || localPath.endsWith("/.") || localPath.endsWith("/")){
            localPath = Path.concat(localPath, Path.basename(path));
        }
        OutputStream output = new BufferedOutputStream(new FileOutputStream(localPath));
        InputStream input = new BufferedInputStream(client.getReadStream(path));

        while(true){
            int c = input.read();
            if(c < 0) break;
            output.write(c);
        }

        output.close();
    }

    @Override
    public void get(String path, String localPath) throws Exception {
        path = init(path);
        _get(path, localPath);
    }

    @Override
    public void getall(String path, String localPath) throws Exception {
        path = init(path);
        Stack<String[]> sk = new Stack<String[]>();
        sk.push(new String[]{path, localPath});
        while(!sk.empty()){
            String[] cur = sk.pop();
            String p = cur[0], lp = cur[1];
            DirectoryEntry en = client.getDirectoryEntry(p);
            if(en.type == DirectoryEntryType.DIRECTORY){
                if(!Path.exists(lp)){
                    Path.mkdir(lp);
                }

                for(FileEntry d : _ls(p)){
                    String name = d.name;
                    sk.push(new String[]{Path.concat(p, name), Path.concat(lp, name)});
                }
            }else if (en.type == DirectoryEntryType.FILE){
                _get(p, lp);
            }
        }
    }

    @Override
    public void putall(String path, String localPath) throws Exception {
        path = init(path);
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

    private String init(String path) throws Exception {
        client = ADLStoreClient.createClient(config.account, provider);
        if (path.startsWith(ADLPREFIX)) {
            URI uri = new URI(path);
            String pathAccount = uri.getHost();
            if (!pathAccount.equals(config.account) || client == null) {
                client = ADLStoreClient.createClient(pathAccount, provider);
            }
            return uri.getPath();
        }

        return Path.reduce(Path.concat(config.currentDirectory, path));
    }
}
