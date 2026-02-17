package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.Date;

public class Stage implements Serializable {

    public static void add(String fileName) {
        File staged = Utils.join(Main.CWD, fileName);
        if (!staged.isFile()) {
            throw new GitletException("File does not exist.");
        }
        File added = Utils.join(Main.STAGEADD, fileName);
        File removed = Utils.join(Main.STAGEREMOVE, fileName);
        File same = Utils.join(Main.STAGESAME, fileName);
        File addedBlob = Utils.join(Main.BLOBS, fileName);
        if (removed.isFile()) {
            String commitHash = Utils.readContentsAsString(removed);
            try {
                same.createNewFile();
                Utils.writeContents(same, commitHash);
                Stage.restrictedDelete(removed);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String addedContent =
                Utils.readContentsAsString(Utils.join(Main.CWD, fileName));
        String fileHash = Utils.sha1(fileName + addedContent);
        String recentHash = null;
        if (same.isFile()) {
            recentHash = Utils.readContentsAsString(same);
        }
        if (recentHash != null && recentHash.equals(fileHash)) {
            if (added.isFile()) {
                restrictedDelete(addedBlob);
                restrictedDelete(added);
            }
        } else {
            if (!added.isFile()) {
                try {
                    added.createNewFile();
                    addedBlob.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Blob newFile = new Blob(fileName, addedContent, fileHash);
            Utils.writeObject(addedBlob, newFile);
        }
    }

    public static void remove(String fileName) {
        File added = Utils.join(Main.STAGEADD, fileName);
        File removed = Utils.join(Main.STAGEREMOVE, fileName);
        File same = Utils.join(Main.STAGESAME, fileName);
        File staged = Utils.join(Main.CWD, fileName);
        if (!added.isFile() && !same.isFile()) {
            throw new GitletException("No reason to remove the file.");
        }
        if (added.isFile()) {
            File addedBlob = Utils.join(Main.BLOBS, fileName);
            restrictedDelete(addedBlob);
            restrictedDelete(added);
        }
        if (same.isFile()) {
            String recentHash = Utils.readContentsAsString(same);
            try {
                removed.createNewFile();
                Utils.writeContents(removed, recentHash);
            } catch (IOException e) {
                e.printStackTrace();
            }
            restrictedDelete(staged);
            restrictedDelete(same);
        }
    }

    public static void makeCommit(String message, String time) {
        int additions = Utils.plainFilenamesIn(Main.STAGEADD).size();
        int removals = Utils.plainFilenamesIn(Main.STAGEREMOVE).size();
        if (additions == 0 && removals == 0) {
            throw new GitletException("No changes added to the commit.");
        } else if (message.equals("")) {
            throw new GitletException("Please enter a commit message");
        }
        Commit newCommit = new Commit(message, time, getHeadCommitStr());
        newCommit.createFile();
        newCommit.serialize();
    }

    public static void makeMergeCommit(String message,
                                       String time, String secondParent) {
        int additions = Utils.plainFilenamesIn(Main.STAGEADD).size();
        int removals = Utils.plainFilenamesIn(Main.STAGEREMOVE).size();
        if (additions == 0 && removals == 0) {
            throw new GitletException("No changes added to the commit.");
        } else if (message.equals("")) {
            throw new GitletException("Please enter a commit message");
        }
        Commit newCommit = new Commit(message, time,
                getHeadCommitStr(), secondParent);
        newCommit.createFile();
        newCommit.serialize();
    }

    public static void checkout1(String fileName) {
        Commit head = getHeadCommit();
        if (head.get(fileName) == null) {
            throw new GitletException("File does not exist in that commit.");
        }
        String prevContent = head.getFileContent(fileName);
        Utils.writeContents(Utils.join(Main.CWD, fileName), prevContent);

    }

    public static void checkout2(String commitHash, String fileName) {
        if (commitHash.length() == 8) {
            commitHash = shortened(commitHash);
        }
        try {
            Commit prevCommit = getCommit(commitHash);
            if (prevCommit.get(fileName) == null) {
                throw new GitletException("File does "
                        +   "not exist in that commit.");
            }
            String prevContent = prevCommit.getFileContent(fileName);
            Utils.writeContents(Utils.join(Main.CWD, fileName), prevContent);
        } catch (GitletException a) {
            System.out.println(a.getMessage());
            System.exit(0);
        }
    }

    public static void checkout3(String branchName) {
        File checkedOutBranch = Utils.join(Main.BRANCHES, branchName);
        if (!checkedOutBranch.isFile()) {
            throw new GitletException("No such branch exists.");
        } else if (getCurrentBranch().equals(branchName)) {
            throw new GitletException("No need to "
                    + "checkout the current branch.");
        }
        String branchHash = Utils.readContentsAsString(checkedOutBranch);
        Commit checkedOut = getCommit(branchHash);
        Set<String> blobNamesSet = checkedOut.getBlobNames();
        Iterator<String> blobNames = blobNamesSet.iterator();
        String blobName, blobHash;
        File blobLocation, trackedLocation;
        while (blobNames.hasNext()) {
            blobName = blobNames.next();
            blobLocation = Utils.join(Main.CWD, blobName);
            trackedLocation = Utils.join(Main.STAGESAME, blobName);
            if (blobLocation.isFile() && !trackedLocation.isFile()) {
                throw new GitletException("There is an untracked"
                + "file in the way; delete it, or add and commit it first.");
            }
        }
        List<String> allAdded = Utils.plainFilenamesIn(Main.STAGEADD);
        List<String> allRemoved = Utils.plainFilenamesIn(Main.STAGEREMOVE);
        List<String> allStaged = Utils.plainFilenamesIn(Main.STAGESAME);
        String currFile;
        for (int i = 0; i < allAdded.size(); i++) {
            currFile = allAdded.get(i);
            restrictedDelete(Utils.join(Main.STAGEADD, currFile));
        }
        for (int i = 0; i < allRemoved.size(); i++) {
            currFile = allRemoved.get(i);
            restrictedDelete(Utils.join(Main.STAGEREMOVE, currFile));
        }
        for (int i = 0; i < allStaged.size(); i++) {
            currFile = allStaged.get(i);
            restrictedDelete(Utils.join(Main.STAGESAME, currFile));
            restrictedDelete(Utils.join(Main.CWD, currFile));
        }
        Utils.writeContents(Main.HEAD, branchName);
        blobNames = blobNamesSet.iterator();
        File newFile, newStage;
        while (blobNames.hasNext()) {
            blobName = blobNames.next();
            blobHash = checkedOut.get(blobName);
            newStage = Utils.join(Main.STAGESAME, blobName);
            newFile = Utils.join(Main.CWD, blobName);
            try {
                newStage.createNewFile();
                newFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeContents(newStage, blobHash);
            Utils.writeContents(newFile, getBlob(blobHash).getContent());
        }
    }

    public static void createBranch(String branchName) {
        File newBranch = Utils.join(Main.BRANCHES, branchName);
        if (newBranch.isFile()) {
            throw new GitletException("A branch with "
                    + "that name already exists.");
        }
        String currBranch = getCurrentBranch();
        String currCommit = branchPtr(currBranch);
        try {
            newBranch.createNewFile();
            Utils.writeContents(newBranch, currCommit);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void rmBranch(String branchName) {
        if (getCurrentBranch().equals(branchName)) {
            throw new GitletException("Cannot remove"
                    +   " the current branch.");
        }
        File rmBranch = Utils.join(Main.BRANCHES, branchName);
        if (!rmBranch.exists()) {
            throw new GitletException("A branch with "
                    + "that name does not exist.");
        }
        restrictedDelete(rmBranch);
    }

    public static void reset(String commitName) {
        File branchFile = new File(Main.BRANCHES,
                getCurrentBranch());
        if (Utils.readContentsAsString(branchFile).
                equals(commitName)) {
            return;
        }
        Commit resetCommit = getCommit(commitName);
        Set<String> blobNamesSet = resetCommit.getBlobNames();
        Iterator<String> blobNames = blobNamesSet.iterator();
        String blobName, blobHash;
        File blobLocation, trackedLocation;
        while (blobNames.hasNext()) {
            blobName = blobNames.next();
            blobLocation = Utils.join(Main.CWD, blobName);
            trackedLocation = Utils.join(Main.STAGESAME, blobName);
            if (blobLocation.isFile() && !trackedLocation.isFile()) {
                throw new GitletException("There is an "
                        + "untracked file in the way;"
                        + " delete it, or add and commit it first.");
            }
        }
        List<String> allAdded = Utils.plainFilenamesIn(Main.STAGEADD);
        List<String> allRemoved = Utils.plainFilenamesIn(Main.STAGEREMOVE);
        List<String> allStaged = Utils.plainFilenamesIn(Main.STAGEREMOVE);
        String currFile;
        for (int i = 0; i < allAdded.size(); i++) {
            currFile = allAdded.get(i);
            restrictedDelete(Utils.join(Main.STAGEADD, currFile));
        }
        for (int i = 0; i < allRemoved.size(); i++) {
            currFile = allRemoved.get(i);
            restrictedDelete(Utils.join(Main.STAGEREMOVE, currFile));
        }
        for (int i = 0; i < allStaged.size(); i++) {
            currFile = allStaged.get(i);
            restrictedDelete(Utils.join(Main.STAGESAME, currFile));
            restrictedDelete(Utils.join(Main.CWD, currFile));
        }
        Utils.writeContents(branchFile, commitName);
        blobNames = blobNamesSet.iterator();
        File newFile, newStage;
        while (blobNames.hasNext()) {
            blobName = blobNames.next();
            blobHash = resetCommit.get(blobName);
            newStage = Utils.join(Main.STAGESAME, blobName);
            newFile = Utils.join(Main.CWD, blobName);
            try {
                newStage.createNewFile();
                newFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeContents(newStage, blobHash);
            Utils.writeContents(newFile, getBlob(blobHash).getContent());
        }
    }

    public static String mergeHelper(String branchName) {
        String currBranch = getCurrentBranch();
        File mergedBranch = Utils.join(Main.BRANCHES, branchName);
        List<String> stagedAdd = Utils.plainFilenamesIn(Main.STAGEADD);
        List<String> stageRemoved = Utils.plainFilenamesIn(Main.STAGEREMOVE);
        if (branchName.equals(currBranch)) {
            throw new GitletException("Cannot merge "
                    + "a branch with itself.");
        } else if (!mergedBranch.isFile()) {
            throw new GitletException("A branch with that"
                   + " name does not exist.");
        } else if (stagedAdd.size() != 0 || stageRemoved.size() != 0) {
            throw new GitletException("You have "
                    +  "uncommitted changes.");
        }
        Commit head = getHeadCommit(), given = getCommit(branchPtr(branchName));
        TreeMap<String, Integer> currAncestors = new TreeMap<String, Integer>(),
                givenAncestors = new TreeMap<String, Integer>();
        mergeHelper2(currAncestors, head, 0);
        mergeHelper2(givenAncestors, given, 0);
        if (currAncestors.containsKey(branchPtr(branchName))) {
            throw new GitletException("Given branch is an "
                    + "ancestor of the current branch");
        } else if (givenAncestors.containsKey(getHeadCommitStr())) {
            checkout3(branchName);
            throw new GitletException("Current branch fast-forwarded.");
        }
        String lowest = null;
        int lowestDist = 6969;
        ArrayList<String> givenAncestorList =
                new ArrayList<String>(givenAncestors.keySet());
        for (int i = 0; i < givenAncestorList.size(); i++) {
            String currAncestor = givenAncestorList.get(i);
            if (currAncestors.containsKey(currAncestor)) {
                if (currAncestors.get(currAncestor) < lowestDist) {
                    lowestDist = currAncestors.get(currAncestor);
                    lowest = currAncestor;
                }
            }
        }
        if (lowest == null) {
            throw new GitletException("Chupapi Munanyo??");
        }
        return lowest;
    }

    public static void mergeHelper2(TreeMap<String,
            Integer> ancestors, Commit currCommit, int dist) {
        String currHash = currCommit.getHash();
        String parent1 = currCommit.getParent(),
                parent2 = currCommit.getParent2();
        ancestors.put(currHash, dist);
        if (!currCommit.getParent().equals("")) {
            mergeHelper2(ancestors, getCommit(parent1), dist + 1);
        }
        if (!currCommit.getParent2().equals("")) {
            mergeHelper2(ancestors, getCommit(parent2), dist + 1);
        }
    }

    public static void merge(String branchName) {
        boolean conflict = false;
        Commit ancestor = getCommit(mergeHelper(branchName));
        Commit given =
                getCommit(Utils.readContentsAsString
                        (Utils.join(Main.BRANCHES, branchName)));
        ArrayList<String> ancestorFileNames =
                new ArrayList<String>(ancestor.getBlobNames());
        ArrayList<String> ancestorBlobHashes =
                new ArrayList<String>(ancestor.getBlobHashes());
        ArrayList<String> givenFileNames =
                new ArrayList<String>(given.getBlobNames());
        ArrayList<String> givenBlobHashes =
                new ArrayList<String>(given.getBlobHashes());
        ArrayList<String> add =
                new ArrayList<String>(), remove = new ArrayList<String>(),
                changed = new ArrayList<String>();
        ArrayList<String> addH =
                new ArrayList<String>(),
                changedH = new ArrayList<String>();
        ArrayList<String> removeOG = new ArrayList<String>(),
                changedOG = new ArrayList<String>();
        ArrayList<ArrayList<String>> styleCheckSucks = new ArrayList<>();
        styleCheckSucks.add(ancestorFileNames);
        styleCheckSucks.add(ancestorBlobHashes);
        styleCheckSucks.add(givenFileNames);
        styleCheckSucks.add(givenBlobHashes);
        addFiles(styleCheckSucks, changed, changedH, changedOG, remove,
                removeOG, add, addH);
        verifyConditions(add, changed);
        conflict = mergeAdd(add, addH, conflict);
        conflict = mergeChanged(changed, changedH, changedOG, conflict);
        conflict = mergeRemoved(remove, removeOG, conflict);
        Date currentDate = new Date();
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
        makeMergeCommit("Merged "
                        + branchName + " into " + getCurrentBranch() + ".",
                String.valueOf(currentDate.getTime()), branchPtr(branchName));
    }

    public static void addFiles(ArrayList<ArrayList<String>>
                                        styleCheckSucks,
                                ArrayList<String> changed,
                                ArrayList<String> changedH,
                                ArrayList<String> changedOG,
                                ArrayList<String> remove,
                                ArrayList<String> removeOG,
                                ArrayList<String> add,
                                ArrayList<String> addH
                                ) {
        int ancestorPtr = 0, givenPtr = 0;
        ArrayList<String> ancestorFileNames = styleCheckSucks.get(0);
        ArrayList<String> ancestorBlobHashes = styleCheckSucks.get(1);
        ArrayList<String> givenFileNames  = styleCheckSucks.get(2);
        ArrayList<String> givenBlobHashes  = styleCheckSucks.get(3);

        while (ancestorPtr < ancestorFileNames.size()
                && givenPtr < givenFileNames.size()) {
            String ancestorFile = ancestorFileNames.get(ancestorPtr),
                    givenFile = givenFileNames.get(givenPtr);
            if (ancestorFile.compareTo(givenFile) == 0) {
                if (!ancestorBlobHashes.get(ancestorPtr).
                        equals(givenBlobHashes.get(givenPtr))) {
                    changed.add(givenFile);
                    changedH.add(givenBlobHashes.get(givenPtr));
                    changedOG.add(ancestorBlobHashes.get(ancestorPtr));
                }
                ancestorPtr++;
                givenPtr++;
            } else if (ancestorFile.compareTo(givenFile) < 0) {
                remove.add(ancestorFile);
                removeOG.add(ancestorBlobHashes.get(ancestorPtr));
                ancestorPtr++;
            } else {
                add.add(givenFile);
                addH.add(givenBlobHashes.get(givenPtr));
                givenPtr++;
            }
        }
        while (ancestorPtr < ancestorFileNames.size()) {
            remove.add(ancestorFileNames.get(ancestorPtr));
            removeOG.add(ancestorBlobHashes.get(ancestorPtr));
            ancestorPtr++;
        }
        while (givenPtr < givenFileNames.size()) {
            add.add(givenFileNames.get(givenPtr));
            addH.add(givenBlobHashes.get(givenPtr));
            givenPtr++;
        }
    }

    public static void verifyConditions(ArrayList<String> add,
                                        ArrayList<String> changed) {
        for (int i = 0; i < add.size(); i++) {
            String addedName = add.get(i);
            if (Utils.join(Main.CWD, addedName).isFile()
                    && !Utils.join(Main.STAGESAME, addedName).isFile()) {
                throw new GitletException("There is an untracked "
                        +    "file in the way; delete it, "
                        +     "or add and commit it first.");
            }
        }
        for (int i = 0; i < changed.size(); i++) {
            String changedName = add.get(i);
            if (Utils.join(Main.CWD, changedName).isFile()
                    && !Utils.join(Main.STAGESAME, changedName).isFile()) {
                throw new GitletException("There is an untracked "
                        +  "file in the way; delete it, "
                        +   "or add and commit it first.");
            }
        }
    }

    public static boolean mergeAdd(ArrayList<String> add,
                            ArrayList<String> addH,
                            boolean ogconflict) {
        boolean conflict = false;
        for (int i = 0; i < add.size(); i++) {
            String addedName = add.get(i);
            File isTracked = Utils.join(Main.STAGESAME,
                    addedName), fileLocation = Utils.join(Main.CWD, addedName);
            Blob contents = getBlob(addH.get(i));
            if (!isTracked.isFile()) {
                try {
                    fileLocation.createNewFile();
                    Utils.writeContents(fileLocation, contents.getContent());
                    add(addedName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (!addH.get(i).equals(Utils.
                    readContentsAsString(isTracked))) {
                String headContents = Utils.readContentsAsString(fileLocation);
                String merged = "<<<<<<< HEAD\n"
                        + headContents + "=======\n"
                        + contents.getContent() + ">>>>>>>\n";
                Utils.writeContents(fileLocation, merged);
                conflict = true;
                add(addedName);
            }
        }
        return (conflict || ogconflict);
    }

    public static boolean mergeChanged(ArrayList<String> changed,
                                      ArrayList<String> changedH,
                                      ArrayList<String> changedOG,
                                      boolean ogconflict) {
        boolean conflict = false;
        for (int i = 0; i < changed.size(); i++) {
            String changedName = changed.get(i);
            File isTracked = Utils.join(Main.STAGESAME, changedName),
                    fileLocation = Utils.join(Main.CWD, changedName);
            Blob contents = getBlob(changedH.get(i));
            if (!isTracked.isFile()) {
                try {
                    conflict = true;
                    fileLocation.createNewFile();
                    String merged = "<<<<<<< HEAD\n"
                            + "=======\n" + contents.getContent() + ">>>>>>>\n";
                    Utils.writeContents(fileLocation, merged);
                    add(changedName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (Utils.readContentsAsString(isTracked)
                    .equals(changedOG.get(i))) {
                Utils.writeContents(fileLocation, contents.getContent());
                add(changedName);
            } else if (!Utils.readContentsAsString(isTracked).
                    equals(changedH.get(i))) {
                conflict = true;
                String headContents = Utils.
                        readContentsAsString(fileLocation);
                String merged = "<<<<<<< HEAD\n"
                        + headContents + "=======\n"
                        + contents.getContent() + ">>>>>>>\n";
                Utils.writeContents(fileLocation, merged);
                add(changedName);
            }
        }
        return (conflict || ogconflict);
    }

    public static String shortened(String shorten) {
        File temp = Utils.join(Utils.join(Main.SHORTCUTS, shorten));
        return Utils.readContentsAsString(temp);
    }

    public static boolean mergeRemoved(ArrayList<String> remove,
                                       ArrayList<String> removeOG,
                                       boolean ogconflict) {
        boolean conflict = false;
        for (int i = 0; i < remove.size(); i++) {
            String removedName = remove.get(i);
            File isTracked = Utils.join(Main.STAGESAME, removedName),
                    fileLocation = Utils.join(Main.CWD, removedName);
            if (isTracked.isFile()) {
                if (Utils.readContentsAsString(isTracked).
                        equals(removeOG.get(i))) {
                    remove(removedName);
                } else {
                    conflict = true;
                    String headContents = Utils.
                            readContentsAsString(fileLocation);
                    String merged = "<<<<<<< HEAD\n"
                            + headContents + "=======\n" + ">>>>>>>\n";
                    Utils.writeContents(fileLocation, merged);
                    add(removedName);
                }
            }
        }
        return (conflict || ogconflict);
    }

    static boolean restrictedDelete(File file) {
        if (!file.isDirectory()) {
            return file.delete();
        } else {
            return false;
        }
    }

    static Commit getHeadCommit() {
        String currBranch = getCurrentBranch();
        String currCommit = branchPtr(currBranch);
        File commitLocation = Utils.join(Main.COMMITS, currCommit);
        return Utils.readObject(commitLocation, Commit.class);
    }
    static String getHeadCommitStr() {
        String currBranch = getCurrentBranch();
        String currCommit = branchPtr(currBranch);
        return currCommit;
    }

    static Commit getCommit(String commitHash) {
        if (commitHash.length() == 6) {
            commitHash = shortened(commitHash);
        }
        File commitLocation = Utils.join(Main.COMMITS, commitHash);
        if (!commitLocation.isFile()) {
            throw new GitletException("No commit with that id exists.");
        }
        return Utils.readObject(commitLocation, Commit.class);
    }

    static Blob getBlob(String blobHash) {
        File blobLocation = Utils.join(Main.BLOBS, blobHash);
        return Utils.readObject(blobLocation, Blob.class);
    }

    static String getCurrentBranch() {
        String currBranch = Utils.readContentsAsString(Main.HEAD);
        return currBranch;
    }

    static String branchPtr(String branch) {
        return Utils.readContentsAsString(Utils.join(Main.BRANCHES, branch));
    }

}
