package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static gitlet.Utils.sha1;

/**
 * Created by monsg on 7/12/2017.
 */
public class Commit implements Serializable {

    private String hashName;
    private String parentHash;
    private String date;
    private String log;
    private HashMap<String, String> fileMap; // wug.txt -> HASHofBLOB


    // Constructor
    public Commit(String parentHash, String log, HashMap<String, String> fileMap) {
        this.parentHash = parentHash;
        this.log = log;
        SimpleDateFormat mydate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        this.date = mydate.format(now);
        this.fileMap = fileMap;
        String hash = "";
        if (parentHash != null) {
            hash += parentHash + date + log;
        } else {
            hash += date + log;
        }
        this.hashName = sha1(hash);
    }

    public String getHashName() {
        return hashName;
    }

    public String getParentHash() {
        return parentHash;
    }

    public void setParentHash(String parentHash) {
        this.parentHash = parentHash;
    }

    public String getDate() {
        return date;
    }

    public String getLog() {
        return log;
    }

    public HashMap<String, String> getFileMap() {
        return fileMap;
    }
}
