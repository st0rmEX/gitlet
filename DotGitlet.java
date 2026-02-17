package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

public class DotGitlet {

    public static void initGitlet() {
        if (Main.DOTGITLET.isDirectory()) {
            throw new GitletException("Gitlet version-control system already "
                    + "exists in the current directory.");
        }
        Main.DOTGITLET.mkdir();
        Main.BLOBS.mkdir();
        Main.COMMITS.mkdir();
        Main.BRANCHES.mkdir();
        Main.STAGE.mkdir();
        Main.STAGEADD.mkdir();
        Main.STAGEREMOVE.mkdir();
        Main.STAGESAME.mkdir();
        Main.SHORTCUTS.mkdir();
        try {
            Main.HEAD.createNewFile();
            Utils.join(Main.BRANCHES, "master").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Commit initial = new Commit("initial commit", "0");
        Utils.writeContents(Main.HEAD,  "master");
        initial.createFile();
        initial.serialize();
    }

    public static void log() {
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        Commit currCommit = null;
        do {
            if (currCommit == null) {
                currCommit = Stage.getHeadCommit();
            } else {
                String parentCommit = currCommit.getParent();
                currCommit = Stage.getCommit(parentCommit);
            }
            printCommit(currCommit, dateFormat);
        } while (!currCommit.initialCommit());
    }

    public static void globalLog() {
        List<String> allCommits = Utils.plainFilenamesIn(Main.COMMITS);
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        String commitStr;
        Commit currCommit;
        for (int i = 0; i < allCommits.size(); i++) {
            commitStr = allCommits.get(i);
            currCommit = Stage.getCommit(commitStr);
            printCommit(currCommit, dateFormat);
        }
    }

    public static void find(String message) {
        List<String> allCommits = Utils.plainFilenamesIn(Main.COMMITS);
        String commitStr;
        Commit currCommit;
        boolean brandonIsDiamond = true;
        for (int i = 0; i < allCommits.size(); i++) {
            commitStr = allCommits.get(i);
            currCommit = Stage.getCommit(commitStr);
            if (currCommit.getMessage().equals(message)) {
                brandonIsDiamond = false;
                System.out.println(commitStr);
            }
        }
        if (brandonIsDiamond) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    public static void status() {
        List<String> branches = Utils.plainFilenamesIn(Main.BRANCHES);
        String headBranch = Stage.getCurrentBranch();
        System.out.println("=== Branches ===");
        for (int i = 0; i < branches.size(); i++) {
            String currBranch = branches.get(i);
            if (currBranch.equals(headBranch)) {
                System.out.print("*");
            }
            System.out.println(currBranch);
        }
        List<String> added = Utils.plainFilenamesIn(Main.STAGEADD);
        List<String> removed = Utils.plainFilenamesIn(Main.STAGEREMOVE);
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (int i = 0; i < added.size(); i++) {
            System.out.println(added.get(i));
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (int i = 0; i < removed.size(); i++) {
            System.out.println(removed.get(i));
        }
        System.out.println();
        statusHelper1();
    }

    public static void statusHelper1() {
        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> previous = Utils.plainFilenamesIn(Main.STAGESAME);
        List<String> cwd = Utils.plainFilenamesIn(Main.CWD);
        Iterator<String> prev = previous.listIterator();
        Iterator<String> curr = cwd.listIterator();
        ArrayList<String> changes = new ArrayList<String>();
        ArrayList<String> untracked = new ArrayList<String>();
        String oldFile = null, newFile = null;
        boolean advanceOld = true, advanceNew = true;
        while (prev.hasNext() && curr.hasNext()) {
            if (advanceOld) {
                oldFile = prev.next();
            }
            if (advanceNew) {
                newFile = curr.next();
            }
            if (!oldFile.equals(newFile)) {
                if (oldFile.compareTo(newFile) < 0) {
                    changes.add(oldFile);
                    advanceOld = false;
                    advanceNew = true;
                } else {
                    changes.add(newFile);
                    advanceOld = true;
                    advanceNew = false;
                }
            } else {
                changes.add(newFile);
                advanceOld = true;
                advanceNew = true;
            }
        }
        while (prev.hasNext()) {
            changes.add(prev.next());
        }
        while (curr.hasNext()) {
            changes.add(curr.next());
        }
        statusHelper2(changes, untracked);
    }

    public static void statusHelper2(ArrayList<String> changes,
                                     ArrayList<String> untracked) {
        String fileName, oldHash, newHash;
        File prevDir, currDir, addDir;
        for (int i = 0; i < changes.size(); i++) {
            fileName = changes.get(i);
            currDir = Utils.join(Main.CWD, fileName);
            prevDir = Utils.join(Main.STAGESAME, fileName);
            addDir = Utils.join(Main.STAGEADD, fileName);
            if (!currDir.isFile()) {
                System.out.println(fileName + " (deleted)");
            } else if (!prevDir.isFile() && !addDir.isFile()) {
                untracked.add(fileName);
            } else {
                if (addDir.isFile()) {
                    Blob oldBlob = Utils.readObject(
                            Utils.join(Main.BLOBS, fileName), Blob.class);
                    oldHash = oldBlob.getHash();
                } else {
                    oldHash = Utils.readContentsAsString(prevDir);
                }
                newHash = Utils.sha1(fileName
                        + Utils.readContentsAsString(currDir));
                if (!oldHash.equals(newHash)) {
                    System.out.println(fileName + " (modified)");
                }
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (int i = 0; i < untracked.size(); i++) {
            System.out.println(untracked.get(i));
        }
    }

    public static void printCommit(Commit currCommit,
                                   SimpleDateFormat dateFormat) {
        System.out.println("===");
        System.out.println("commit " + currCommit.getHash());
        if (!currCommit.getParent2().equals("")) {
            System.out.println("Merge: "
                    + currCommit.getParent().substring(0, 7)
                    + " " + currCommit.getParent2().substring(0, 7));
        }
        Long currTime = Long.parseLong(currCommit.getTime());
        Date commitDate = new Date(currTime);
        System.out.println("Date: " + dateFormat.format(commitDate));
        System.out.println(currCommit.getMessage());
        System.out.println();
    }
}
