package gitlet;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static gitlet.Utils.*;


/**
 * Created by monsg on 7/17/2017.
 */
public class Gitlet implements Serializable {
    private HashMap<String, String> branches = new HashMap<>();
    private String head; // hash of commit file
    private String headBranch;
    private String workingDir;

    private HashMap<String, String> stageAdd = new HashMap<>(); // HASH OF BLOBS
    private ArrayList<String> stageRemove = new ArrayList<String>(); // HASH OF BLOBS
    private ArrayList<String> allCommitsIDs = new ArrayList<>();
    private HashMap<String, String> shortCommitsIDs = new HashMap<>();

    //private String stageTracked = new String[]; // HASH OF BLOBS

    public Gitlet() throws IOException, ClassNotFoundException {
        // if file exists, load gitlet object if not create new one
        workingDir = System.getProperty("user.dir");
        File prevState = new File(workingDir, ".gitlet/gitletState");
//        System.out.println(prevState.getAbsolutePath());
        if (prevState.exists()) {
//            System.out.println("c");
            Gitlet prevGitlet = (Gitlet) deserialize(Utils.readContents(prevState));
            this.branches = prevGitlet.branches;
            this.head = prevGitlet.head;
            this.headBranch = prevGitlet.headBranch;
            this.workingDir = prevGitlet.workingDir;
            this.stageAdd = prevGitlet.stageAdd;
            this.stageRemove = prevGitlet.stageRemove;
            this.allCommitsIDs = prevGitlet.allCommitsIDs;
            this.shortCommitsIDs = prevGitlet.shortCommitsIDs;
//            saveState();
//            return;
        }
    }

    public void saveState() throws IOException {
        // This creates file pointer for a file in ./gitlet folder named gitletState
        File newGitlet = new File(workingDir, ".gitlet/gitletState");
        // This saves the entire "this" into a file
        writeContents(newGitlet, serialize(this));
    }

    public void init() throws IOException {

        HashMap<String, String> empty = new HashMap<>();
        Commit initialCommit = new Commit(null, "initial commit", empty);


        /* Append "/.gitlet" to whatever path was created above. */
        File gitletdir = new File(workingDir, ".gitlet");
        if (gitletdir.exists()) {
            System.err.println("A gitlet version-control system "
                    + "already exists in the current directory.");

        }
        /* Create the .gitlet directory! */
        gitletdir.mkdir();

        File newCommit = new File(workingDir, ".gitlet/" + initialCommit.getHashName());

        writeContents(newCommit, serialize(initialCommit));

        branches.put("master", initialCommit.getHashName());

        head = branches.get("master");

        headBranch = "master";
        allCommitsIDs.add(initialCommit.getHashName());

        saveState();
    }

    //Gadd
    public void add(String fileName) throws IOException, ClassNotFoundException  {
        File file = new File(workingDir, fileName);

        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        if (stageRemove.contains(fileName)) {
            stageRemove.remove(fileName);
            saveState();
            return;
        }
        // get hash
        String hash = fileName + sha1(readContents(file));
        File temp = new File(workingDir, ".gitlet/" + head);
        // Recover last commit
        Commit target = (Commit) deserialize(Utils.readContents(temp));


        if (stageAdd.containsKey(fileName) && stageAdd.get(fileName).equals(hash)) {
            System.out.println("No change added.");
            return;
        }

        if (hash.equals(target.getFileMap().get(fileName))) {
            //System.out.println(hash);
            //System.out.println("hash:");
            saveState();
            return;
        }

        File blob = new File(workingDir, ".gitlet/" + hash);
        // Makes copy of file andn puts it in blob with name "hash"
        writeContents(blob, readContents(file));
        // Puts staged file in staging area
        stageAdd.put(fileName, hash); // key=fileName, value=hash


        saveState();
    }

    //Gcommit
    public void commit(String args) throws IOException, ClassNotFoundException {
        // Deal with edge case File does not exist.
        if (stageAdd.isEmpty() && stageRemove.isEmpty()) {

            System.out.println("No changes added to the commit.");

            return;
        }

        String log = args;
        HashMap<String, String> blobMap = new HashMap<>();
        // Copy current commit's dictionary and update it with the s
        // taging area, save it to new commit
        File temp = new File(workingDir, ".gitlet/" + head);
        Commit parent = (Commit) deserialize(Utils.readContents(temp));

        if (parent.getFileMap() != null) {
            blobMap.putAll(parent.getFileMap());
        }

        blobMap.putAll(stageAdd); // TODO stageRemove
        for (String i: stageRemove) {
            blobMap.remove(i);
        }

        stageAdd = new HashMap<>();
        stageRemove = new ArrayList<>();

        Commit c = new Commit(head, log, blobMap);

        File newCommit = new File(workingDir, ".gitlet/" + c.getHashName());
        writeContents(newCommit, serialize(c));

        allCommitsIDs.add(c.getHashName());
        String shortID = c.getHashName().substring(0, 8);
        shortCommitsIDs.put(shortID, c.getHashName());

        head = c.getHashName();

        branches.put(headBranch, head);

        saveState();


    }

    //// Grm
    public void rm(String fileName) throws IOException, ClassNotFoundException {
        // if file is alread in stageRemove, just return
        if (stageRemove.contains(fileName)) {
            System.out.println("Already in remove list");
            return;
        }

        if (stageAdd.containsKey(fileName)) {
            stageAdd.remove(fileName);
            saveState();
            return;
        }
        // Get path to last commit
        File temp = new File(workingDir, ".gitlet/" + head);
        // Recover last commit
        Commit target = (Commit) deserialize(Utils.readContents(temp));

        File fileToDelete = new File(workingDir, fileName);

        if (target.getFileMap().containsKey(fileName)) {
            stageRemove.add(fileName);

            if (fileToDelete.exists()) {

                fileToDelete.delete();
            }
            if (stageAdd.containsKey(fileName)) {
                stageAdd.remove(fileName);
            }
//            target.getFileMap().remove(fileName);
            saveState();
            return;
        } else if (stageAdd.containsKey(fileName)) {
            stageAdd.remove(fileName);
            saveState();
            return;
        }
        System.err.println("No reason to remove the file.");
    }

    //Gcheck
    public void checkout(String[] args) {
        try {
            switch (args.length) {
                case 2:
                    checkoutBranch(args[1]);
                    saveState();
                    break;
                case 3:
                    checkoutFile(args[2]);
                    saveState();
                    break;
                case 4:
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                    }
                    checkoutCommit(args[1], args[3]);
                    saveState();
                    break;
                default:
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    // GchedkBr
    public void checkoutBranch(String branch) throws IOException, ClassNotFoundException {
        // Checks for all three failure cases
        /// If branch doesn't exist
        if (!branches.containsKey(branch)) {
            System.out.println("No such branch exists.");
            return;
        }
        /// If branch is current branch
        if (headBranch.equals(branch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        // Recover branch commit
        File temp = new File(workingDir, ".gitlet/" + branches.get(branch));
        Commit branchCommit = (Commit) deserialize(Utils.readContents(temp));
        // Recover last commit
        File temp2 = new File(workingDir, ".gitlet/" + head);
        Commit currBranch = (Commit) deserialize(Utils.readContents(temp2));

        /// Check for untracked files
        for (String a : Utils.plainFilenamesIn(workingDir)) {
//            if (!currBranch.fileMap.containsKey(a) ) {
//                System.out.println("There is an untracked
// file in the way; delete it or add it first.1111");
//                return;
//            }
            File file = new File(workingDir, a);
            String fileHash = a + sha1(readContents(file));
            if (branchCommit.getFileMap().containsKey(a)
                    && !branchCommit.getFileMap().get(a).equals(fileHash)
                    && !currBranch.getFileMap().containsKey(a)
                    && !a.equals(".gitignore") && !a.equals("proj2.iml")) {
                System.out.println("There is an untracked"
                        + " file in the way; delete it or add it first.");
                return;
            }
        }

//        for(String a : Utils.plainFilenamesIn(workingDir)) {

//        }
        ArrayList<String> holder = new ArrayList<>();
        for (String a : currBranch.getFileMap().keySet()) {
            //Replace
            if (branchCommit.getFileMap().containsKey(a)) {
                /// Overwrite!
                // targets blob a hash
                String blobHash = branchCommit.getFileMap().get(a);
                // creates file from blob a hash saved in /.gitlet
                File blobFile = new File(workingDir + "/.gitlet", blobHash);
                // creates file in working directory to be replaced
                File fileToReplace = new File(workingDir, a);
                writeContents(fileToReplace, readContents(blobFile));
            }
            // Delete
            File fileToDelete = new File(workingDir, a);
            fileToDelete.delete();
        }
        //CREATE new files
        for (String a : branchCommit.getFileMap().keySet()) {
            // targets blob a hash
            String blobHash = branchCommit.getFileMap().get(a);
            // creates file from blob a hash saved in /.gitlet
            File blobFile = new File(workingDir + "/.gitlet", blobHash);
            // creates file in working directory to be replaced
            File fileToReplace = new File(workingDir, a);
            writeContents(fileToReplace, readContents(blobFile));
        }

        /// Set head to point to new branch
        head = branches.get(branch);
        headBranch = branch;

        saveState();
    }

    public String[] filesInDir(String dirName) {
        File directory = new File(dirName);
        String[] fList = directory.list();
        return fList;
    }

    public void checkoutCommit(String commit, String filename)
            throws IOException, ClassNotFoundException {
        // Check if commit exist
//        System.out.println("All commits: ");
//        System.out.println(allCommitsIDs);
//        System.out.println("\n");

        if (allCommitsIDs.contains(commit)
                || shortCommitsIDs.containsKey(commit)) {
            // Get commit
            if (shortCommitsIDs.containsKey(commit)) {
                commit = shortCommitsIDs.get(commit);
            }
            File temp = new File(workingDir, ".gitlet/" + commit);
            Commit target = (Commit) deserialize(Utils.readContents(temp));
            // if commit found iterate through files in commit
            for (String file : target.getFileMap().keySet()) {
                // If file exist in commit found then:
                if (file.equals(filename)) {
                    String blobHash = target.getFileMap().get(filename);
                    File blobFile = new File(workingDir + "/.gitlet", blobHash);
                    File fileToReplace = new File(workingDir, filename);
                    writeContents(fileToReplace, readContents(blobFile));
                    return;
                }
            }
            System.out.println("File does not exist in that commit.");
            return;
        } else {

//            System.out.println("commit: ");
//            System.out.println(commit);
            System.out.println("No commit with that id exists.");
            return;
        }
    }



    ///Gcheckfile
    public void checkoutFile(String filename)
            throws IOException, ClassNotFoundException {
        // Get path to last commit
        File temp = new File(workingDir, ".gitlet/" + head);
        // Recover last commit
        Commit target = (Commit) deserialize(Utils.readContents(temp));


        // Check if last commit has pointer to that file
        if (!target.getFileMap().containsKey(filename)) {
            // TODO check if filename has path
            return;
        }
        // Get pointer of last commit's file version
        String blobHash = target.getFileMap().get(filename);
        // Get path to commit's file version
        File blobFile = new File(workingDir + "/.gitlet", blobHash);

        File fileToReplace = new File(workingDir, filename);
        // Write contents of commit's file version to file to replace
        writeContents(fileToReplace, readContents(blobFile));
    }

    //Glog
    public void log() throws IOException, ClassNotFoundException {
        helpLog(head);
        saveState();
    }

    public void helpLog(String currCom) throws ClassNotFoundException, IOException {
        File temp = new File(workingDir + "/.gitlet", currCom);
        Commit curr = (Commit) deserialize(Utils.readContents(temp));

        if (curr.getParentHash() == null) {
            System.out.println("===");
            System.out.println("Commit " + curr.getHashName());
            System.out.println(curr.getDate());
            System.out.println(curr.getLog());
            System.out.println("");
            return;
        }

        System.out.println("===");
        System.out.println("Commit " + curr.getHashName());
        System.out.println(curr.getDate());
        System.out.println(curr.getLog());
        System.out.println("");

        helpLog(curr.getParentHash());
    }

    //Gglog
    public void globallog() throws IOException, ClassNotFoundException {
        // same as log but now we go to each branch and print history
        // until we get to SHA 1 we've already seen
        //Set<String> keyNames = branches.keySet();
        for (String commmitID : allCommitsIDs) {
            File temp = new File(workingDir, ".gitlet/" + commmitID);
            Commit curr = (Commit) deserialize(Utils.readContents(temp));

            System.out.println("===");
            System.out.println("Commit " + curr.getHashName());
            System.out.println(curr.getDate());
            System.out.println(curr.getLog());
            System.out.println("");

        }
        saveState();
    }


    ArrayList<String> shaNames2 = new ArrayList<>();
    ArrayList<String> seenCommits = new ArrayList<>();


    //Gfind
    public void find(String comMessage) throws IOException, ClassNotFoundException {

//        for(String a : branches.keySet()) {
//            helpfind(branches.get(a), comMessage);
//        }
//        if(seenCommits.isEmpty()){
//
//            System.out.println("Found no commit with that message.");
//
//        }
        boolean found = false;
        for (String commmitID : allCommitsIDs) {
            File temp = new File(workingDir, ".gitlet/" + commmitID);
            Commit commit = (Commit) deserialize(Utils.readContents(temp));
            if (commit.getLog().equals(comMessage)) {
                found = true;
                System.out.println(commit.getHashName());
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }

        saveState();
    }


    public void helpfind(String currCom, String comMessage) throws IOException,
            ClassNotFoundException {
        if (currCom == null) {
            return;
        }

        File temp = new File(workingDir + "/.gitlet/", currCom);
        Commit curr = (Commit) deserialize(Utils.readContents(temp));

        // checks an ArrayList that will be populated with already
        // seen commit SHA1 values to make sure we don't go over the same
        // commit twice
        if (!shaNames2.contains(curr.getHashName())) {
            // prints out the commit SHA1 if the commit message
            // matches the message we're looking for

            if (curr.getLog().equals(comMessage)) {

                System.out.println(curr.getHashName());

                seenCommits.add(curr.getHashName());
            }
            // adds current commits SHA1 value to
            // ArrayList Names2
            shaNames2.add(curr.getHashName());
        }
        // recursively calls helpfind on parent
        helpfind(curr.getParentHash(), comMessage);
    }

    /// Gstat
    public void status() throws IOException {
        System.out.println("=== Branches ===");
        List<String> sArray = new ArrayList<String>(branches.keySet());
        java.util.Collections.sort(sArray);

        //System.out.println(sArray);
        for (String i : sArray) {
            if (i.equals(headBranch)) {
                System.out.println("*" + i);
            } else {
                System.out.println(i);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        sArray = new ArrayList<String>(stageAdd.keySet());
        java.util.Collections.sort(sArray);
        for (String i : sArray) {
            System.out.println(i);
        }

        System.out.println();

        System.out.println("=== Removed Files ===");
        Collections.sort(stageRemove);
        for (String i : stageRemove) {
            System.out.println(i);
        }

        System.out.println();

        // OPTIONAL BELOW
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");

        saveState();
    }

    // new HashMap that maps Branches to the intersection
    // point (commit) between this and the next branch
    private HashMap<String, String> branchToIntCom = new HashMap<>(); //Branch --> SHA1
    private int count;

    //Gbranch
    public void branch(String branchName) throws IOException {
        // adds element to hashMap mapping a new branchName
        // to the SHA1 of the current commit
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }

        // does not checkout to new branch!
        branches.put(branchName, head);

        // populates branchToIntCom with the BranchName
        // and the corresponding intersection point (commit)
        branchToIntCom.put(branchName, head);
        saveState();
    }

    // Grmbranch
    public void rmbranch(String branchName) throws IOException, ClassNotFoundException {
        if (head.equals(branches.get(branchName))) {
            throw new IllegalArgumentException("Cannot Remove the correct branch.");
        }
        helprmBranch(branches.get(branchName), branchName);
        if (count == 0) {
            throw new IllegalArgumentException("A branch with that name does not exist");
        }
        saveState();
    }

    public void helprmBranch(String currCom, String branchName) throws IOException,
            ClassNotFoundException {
        File temp = new File(workingDir, ".gitlet/" + currCom);
        Commit curr = (Commit) deserialize(Utils.readContents(temp));
        // checks to see if the currCommit's parent is the intersection point (commit)
        // and removes the pointer to it's parent if true,
        // recursively calls helprmBranch on it's parent if false
        if (curr.getParentHash().equals(branchToIntCom.get(branchName))) {
            curr.setParentHash(null);
            branches.remove(branchName);
            branchToIntCom.remove(branchName);
            count++;
        } else {
            helprmBranch(curr.getParentHash(), branchName);
        }
    }









    public void reset(String resetComID) throws IOException, ClassNotFoundException {
        // Commit not found
        if (!allCommitsIDs.contains(resetComID)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        /// Check for untracked files
        // Recover last commit
        File temp2 = new File(workingDir, ".gitlet/" + head);
        Commit currBranch = (Commit) deserialize(Utils.readContents(temp2));

        // Recover reset commit
        File temp = new File(workingDir, ".gitlet/" + resetComID);
        Commit resetCommit = (Commit) deserialize(Utils.readContents(temp));

//        System.out.println("stage add");
//        System.out.println(stageAdd.keySet());
        for (String a : Utils.plainFilenamesIn(workingDir)) {
            File file = new File(workingDir, a);
            String fileHash = a + sha1(readContents(file));
            if (resetCommit.getFileMap().containsKey(a)
                    && !resetCommit.getFileMap().get(a).equals(fileHash)
                    && !currBranch.getFileMap().containsKey(a)) {
                System.out.println("There is an untracked "
                        + "file in the way; delete it or add it first.");
                return;
            }
        }
        for (String a : resetCommit.getFileMap().keySet()) {
            checkoutCommit(resetComID, a);
        }
        // clear staging area
        stageAdd = new HashMap<>();
        head = resetComID;
        branches.remove(headBranch);
        branches.put(headBranch, resetComID);

        saveState();
    }


    // recursive void function, history array maintained in caller
    public void getCommitHistory(ArrayList<String> history, String commitHash) throws IOException,
            ClassNotFoundException {
        if (commitHash == null) {
            return;
        }
        File temp = new File(workingDir, ".gitlet/" + commitHash);
        Commit currOne = (Commit) deserialize(Utils.readContents(temp));
        history.add(currOne.getHashName());
        getCommitHistory(history, currOne.getParentHash());
    }

    // ***************************************** MERGE ********************************************
    // ***************************************** MERGE ********************************************
    // ***************************************** MERGE ********************************************
    // ***************************************** MERGE ********************************************

    // ***************************************** MERGE ********************************************
    // ***************************************** MERGE ********************************************
    // ***************************************** MERGE ********************************************
    // ***************************************** MERGE ********************************************

    // ***************************************** MERGE ********************************************
    // ***************************************** MERGE ********************************************
    // ***************************************** MERGE ********************************************
    // ***************************************** MERGE ********************************************

    public void merge(String givenBranchName) throws IOException,
            ClassNotFoundException {
        if (mergeChecks(givenBranchName)) {
            return;
        }
        boolean conflict = false; // Flag to check if we have a merge conflict (default is false);
        File temp = new File(workingDir, ".gitlet/" + head);
        String branchPointer = head;
        String splitPoint = head;
        String newBranch = branches.get(givenBranchName);
        splitPoint = findingSplit(splitPoint, branchPointer, newBranch);
        if (splitPoint.equals(newBranch)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        } else if (splitPoint.equals(head)) {
            head = newBranch;
            headBranch = givenBranchName;
            System.out.println("Current branch fast-forwarded.");
            saveState();
            return;
        } else {
            File stemp = new File(workingDir, ".gitlet/" + head);
            Commit currBranch = (Commit) deserialize(Utils.readContents(stemp));
            stemp = new File(workingDir, ".gitlet/" + branches.get(givenBranchName));
            Commit givenBranch = (Commit) deserialize(Utils.readContents(stemp));
            stemp = new File(workingDir, ".gitlet/" + splitPoint);
            Commit splitBranch = (Commit) deserialize(Utils.readContents(stemp));
            List<String> currentMod = new ArrayList<String>();
            List<String> otherMod = new ArrayList<String>();
            HashMap<String, String> currentFiles = currBranch.getFileMap();
            HashMap<String, String> otherFiles = givenBranch.getFileMap();
            HashMap<String, String> splitFiles = splitBranch.getFileMap();
            for (String file : currentFiles.keySet()) {
                String fileHash = currentFiles.get(file);
                String splitHash = splitFiles.get(file);
                if (!fileHash.equals(splitHash) && splitFiles.containsKey(file)) {
                    currentMod.add(file);
                }
            }
            for (String file : otherFiles.keySet()) {
                String fileHash = otherFiles.get(file);
                String splitHash = splitFiles.get(file);
                if (!fileHash.equals(splitHash) && splitFiles.containsKey(file)) {
                    otherMod.add(file);
                }
            }
            for (String filename : otherMod) {
                if (currBranch.getFileMap().containsKey(filename)
                        && !currentMod.contains(filename)) {
                    checkoutCommit(givenBranch.getHashName(), filename);
                }
            }
            for (String filename : currentMod) {
                if (givenBranch.getFileMap().containsKey(filename)
                        && !otherMod.contains(filename)) {
                    checkoutCommit(head, filename);
                }
            }
            for (String filename : currentFiles.keySet()) {
                if (!otherFiles.containsKey(filename)
                        && !splitFiles.containsKey(filename)) {
                    checkoutCommit(head, filename);
                }
            }
            for (String filename : otherFiles.keySet()) {
                if (!currentFiles.containsKey(filename)
                        && !splitFiles.containsKey(filename)) {
                    checkoutCommit(givenBranch.getHashName(), filename);
                    stageAdd.put(filename, otherFiles.get(filename));
                }
            }
            for (String filename : splitFiles.keySet()) {
                if (!otherFiles.containsKey(filename)
                        && currentFiles.containsKey(filename) && !currentMod.contains(filename)) {
                    File temp2 = new File(workingDir, filename);
                    if (temp2.exists()) {
                        rm(filename);
                    }
                }
            }
            for (String filename : splitFiles.keySet()) {
                if (!currentFiles.containsKey(filename)
                        && otherFiles.containsKey(filename) && !otherMod.contains(filename)) {
                    File temp2 = new File(workingDir, filename);
                    if (temp2.exists()) {
                        rm(filename);
                    }
                }
            }
            conflict = writeToFiles(conflict, otherMod, currentMod, otherFiles,
                    currentFiles, givenBranch, currBranch, splitBranch);
            if (conflict) {
                System.out.println("Encountered a merge conflict.");
                return;
            }
            commit("Merged " + headBranch + " with " + givenBranchName + ".");
            saveState();
        }
    }

    public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }
    public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public boolean mergeChecks(String branchName) throws IOException, ClassNotFoundException {
        File skoot = new File(workingDir, ".gitlet/" + head);
        Commit currenttBranch = (Commit) deserialize(Utils.readContents(skoot));
        if (!stageAdd.isEmpty() || !stageRemove.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        if (headBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        for (String a : Utils.plainFilenamesIn(workingDir)) {
            if (!currenttBranch.getFileMap().containsKey(a) && !a.equals(".gitignore")
                    && !a.equals("proj2.iml") || !stageAdd.isEmpty()) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                return true;
            }
        }
        return false;
    }

    public String findingSplit(String splitPoint, String branchPointer, String newBranch) throws
            IOException, ClassNotFoundException {
        ArrayList<String> currentBranchHistory = new ArrayList<>();
        ArrayList<String> givenBranchHistory = new ArrayList<>();
        File temp = new File(workingDir, ".gitlet/" + head);
        while (branchPointer != null) {
            temp = new File(workingDir, ".gitlet/" + branchPointer);
            Commit commit = (Commit) deserialize(Utils.readContents(temp));
            currentBranchHistory.add(commit.getHashName());
            branchPointer = commit.getParentHash();
        }
        branchPointer = newBranch;
        while (branchPointer != null) {
            temp = new File(workingDir, ".gitlet/" + branchPointer);
            Commit commit = (Commit) deserialize(Utils.readContents(temp));
            givenBranchHistory.add(commit.getHashName());
            branchPointer = commit.getParentHash();
        }
        boolean found = false;
        for (int i = 0; i < currentBranchHistory.size() && !found; i++) {
            for (int j = 0; j < givenBranchHistory.size() && !found; j++) {
                if (currentBranchHistory.get(i).equals(givenBranchHistory.get(j))) {
                    splitPoint = currentBranchHistory.get(i);
                    found = true;
                }
            }
        }
        return splitPoint;
    }

    public boolean writeToFiles(boolean conflict, List<String> otherMod, List<String> currentMod,
                                HashMap<String, String> otherFiles, HashMap<String,
                                String> currentFiles,
                                Commit givenBranch, Commit currBranch, Commit splitBranch)
                                    throws IOException {
        ArrayList<String> combined = new ArrayList<>();
        for (String filename : currentMod) {
            if ((otherMod.contains(filename)
                    && !otherFiles.get(filename).equals(currentFiles.get(filename)))
                    || !otherMod.contains(filename)) {
                combined.add(filename);
                String givFile = givenBranch.getFileMap().get(filename);
                String curFile = currBranch.getFileMap().get(filename);
                String splFile = splitBranch.getFileMap().get(filename);
                conflict = true;
                String topDiv = "<<<<<<< HEAD\n";
                File currFile = new File(workingDir, ".gitlet/" + curFile);
                String midDiv = "=======\n";
                File givenFile = new File(workingDir, ".gitlet/" + givFile);
                String btmDiv = ">>>>>>>\n";
                String x = "";
                if (currFile.exists()) {
                    byte[] encoded1 = Files.readAllBytes(currFile.toPath());
                    x = new String(encoded1);
                }
                String y = "";
                if (givenFile.exists()) {
                    byte[] encoded2 = Files.readAllBytes(givenFile.toPath());
                    y = new String(encoded2);
                }
                String total = topDiv + x + midDiv + y + btmDiv;
                File outp = new File(workingDir, filename);
                writeContents(outp, total.getBytes());
            }
        }

        for (String filename : otherMod) {
            if ((!combined.contains(filename) && currentMod.contains(filename)
                    && !otherFiles.get(filename).equals(currentFiles.get(filename)))
                    || !currentMod.contains(filename)) {
                combined.add(filename);
                String givFile = givenBranch.getFileMap().get(filename);
                String curFile = currBranch.getFileMap().get(filename);
                String splFile = splitBranch.getFileMap().get(filename);
                conflict = true;
                String topDiv = "<<<<<<< HEAD\n";
                File currFile = new File(workingDir, ".gitlet/" + curFile);
                String midDiv = "=======\n";
                File givenFile = new File(workingDir, ".gitlet/" + givFile);
                String btmDiv = ">>>>>>>\n";
                String x = "";
                if (currFile.exists()) {
                    byte[] encoded1 = Files.readAllBytes(currFile.toPath());
                    x = new String(encoded1);
                }
                String y = "";
                if (givenFile.exists()) {
                    byte[] encoded2 = Files.readAllBytes(givenFile.toPath());
                    y = new String(encoded2);
                }
                String total = topDiv + x + midDiv + y + btmDiv;
                File outp = new File(workingDir, filename);
                writeContents(outp, total.getBytes());
            }
        }
        return conflict;
    }
}
