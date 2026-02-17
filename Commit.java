package gitlet;

import java.io.IOException;
import java.io.Serializable;
import java.util.TreeMap;
import java.util.List;
import java.util.Collection;
import java.util.Set;
import java.util.ArrayList;
import java.io.File;

public class Commit implements Serializable {
    public Commit(String message, String time) {
        _fileNameToHash = new TreeMap<>();
        _time = time;
        _message = message;
        _parentHash = "";
        _parentHash2 = "";
        _hash = generateHash();
        String shortHash = _hash.substring(0, 7);
        File boop = Utils.join(Main.SHORTCUTS, shortHash);
        try {
            boop.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(boop, _hash);
    }
    public Commit(String message, String time, String parent) {
        _fileNameToHash = new TreeMap<String, String>();
        List<String> old = Utils.plainFilenamesIn(Main.STAGESAME);
        List<String> added = Utils.plainFilenamesIn(Main.STAGEADD);
        List<String> removed = Utils.plainFilenamesIn(Main.STAGEREMOVE);
        File blobPtr, newPtr, addedBlob;
        String blobName, blobHash;
        for (int i = 0; i < old.size(); i++) {
            blobName = old.get(i);
            blobPtr = Utils.join(Main.STAGESAME, blobName);
            blobHash = Utils.readContentsAsString(blobPtr);
            _fileNameToHash.put(blobName, blobHash);
        }
        for (int i = 0; i < added.size(); i++) {
            blobName = added.get(i);
            blobPtr = Utils.join(Main.STAGEADD, blobName);
            newPtr = Utils.join(Main.STAGESAME, blobName);
            addedBlob = Utils.join(Main.BLOBS, blobName);
            blobHash = Utils.readObject(addedBlob, Blob.class).getHash();
            _fileNameToHash.put(blobName, blobHash);
            File newLocation = Utils.join(Main.BLOBS, blobHash);
            if (true) {
                addedBlob.renameTo(newLocation);
            } else {
                Stage.restrictedDelete(newLocation);
            }
            if (!newPtr.isFile()) {
                try {
                    newPtr.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Utils.writeContents(newPtr, blobHash);
            Stage.restrictedDelete(blobPtr);
        }
        for (int i = 0; i < removed.size(); i++) {
            blobPtr = Utils.join(Main.STAGEREMOVE, removed.get(i));
            Stage.restrictedDelete(blobPtr);
        }
        _time = time;
        _message = message;
        _parentHash = parent;
        _parentHash2 = "";
        _hash = generateHash();
        String shortHash = _hash.substring(0, 8);
        File boop = Utils.join(Main.SHORTCUTS, shortHash);
        try {
            boop.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(boop, _hash);
    }

    public Commit(String message, String time, String parent,
                  String secondParent) {
        _parentHash2 = secondParent;
        _fileNameToHash = new TreeMap<String, String>();
        List<String> old = Utils.plainFilenamesIn(Main.STAGESAME);
        List<String> added = Utils.plainFilenamesIn(Main.STAGEADD);
        List<String> removed = Utils.plainFilenamesIn(Main.STAGEREMOVE);
        File blobPtr, newPtr, addedBlob;
        String blobName, blobHash;
        for (int i = 0; i < old.size(); i++) {
            blobName = old.get(i);
            blobPtr = Utils.join(Main.STAGESAME, blobName);
            blobHash = Utils.readContentsAsString(blobPtr);
            _fileNameToHash.put(blobName, blobHash);
        }
        for (int i = 0; i < added.size(); i++) {
            blobName = added.get(i);
            blobPtr = Utils.join(Main.STAGEADD, blobName);
            newPtr = Utils.join(Main.STAGESAME, blobName);
            addedBlob = Utils.join(Main.BLOBS, blobName);
            blobHash = Utils.readObject(addedBlob, Blob.class).getHash();
            _fileNameToHash.put(blobName, blobHash);
            File newLocation = Utils.join(Main.BLOBS, blobHash);
            if (!newLocation.isFile()) {
                addedBlob.renameTo(newLocation);
            } else {
                Stage.restrictedDelete(newLocation);
            }
            if (!newPtr.isFile()) {
                try {
                    newPtr.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Utils.writeContents(newPtr, blobHash);
            Stage.restrictedDelete(blobPtr);
        }
        for (int i = 0; i < removed.size(); i++) {
            blobPtr = Utils.join(Main.STAGEREMOVE, removed.get(i));
            Stage.restrictedDelete(blobPtr);
        }
        _time = time;
        _message = message;
        _parentHash = parent;
        _hash = generateHash();
        String shortHash = _hash.substring(0, 7);
        File boop = Utils.join(Main.SHORTCUTS, shortHash);
        try {
            boop.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(boop, _hash);
    }


    public boolean initialCommit() {
        return _parentHash.equals("");
    }

    public boolean isEmpty() {
        return _fileNameToHash.size() == 0;
    }

    private String generateHash() {
        String valuesHash = "";
        if (!isEmpty()) {
            ArrayList<String> valueSet =
                    new ArrayList<String>(_fileNameToHash.values());
            for (int i = 0; i < valueSet.size(); i++) {
                valuesHash += valueSet.get(i);
            }
        }
        String hash = Utils.sha1(_parentHash + _parentHash2
                + valuesHash + _time + _message);
        return hash;
    }

    public String getHash() {
        return _hash;
    }

    public String getTime() {
        return _time;
    }

    public String getMessage() {
        return _message;
    }

    public String getParent() {
        return _parentHash;
    }

    public String getParent2() {
        return _parentHash2;
    }

    public Set<String> getBlobNames() {
        return _fileNameToHash.keySet();
    }

    public Collection<String> getBlobHashes() {
        return _fileNameToHash.values();
    }

    public void createFile() {
        File currCommit = Utils.join(Main.COMMITS, getHash());
        try {
            currCommit.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void serialize() {
        File currCommit = Utils.join(Main.COMMITS, getHash());
        Utils.writeObject(currCommit, this);
        String currBranch = Stage.getCurrentBranch();
        Utils.writeContents(Utils.join(Main.BRANCHES, currBranch), getHash());
    }

    public String get(String fileName) {
        return _fileNameToHash.get(fileName);
    }

    public String getFileContent(String fileName) {
        Blob temp = Utils.readObject(Utils.join(Main.BLOBS,
                get(fileName)), Blob.class);
        return temp.getContent();
    }
    /** Time the commit was created.*/
    private String _time;
    /** Message associated with commit.*/
    private String _message;
    /** Parent hash of commit.*/
    private String _parentHash;
    /** Second or merged parent of commit, for merge commits only.*/
    private String _parentHash2;
    /** Hash of this commit itself.*/
    private String _hash;
    /** TreeMap mapping all tracked files to blob hashes.*/
    private TreeMap<String, String> _fileNameToHash;

}
