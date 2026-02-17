package gitlet;

import java.io.Serializable;

public class Blob implements Serializable {

    public Blob(String name, String content, String hash) {
        _blobName = name;
        _blobContent = content;
        _blobHash = hash;
    }

    public String getContent() {
        return _blobContent;
    }
    public String getHash() {
        return _blobHash;
    }

    /** Stores the contents of the given file.*/
    private String _blobContent;
    /** Hash of file contents.*/
    private String _blobHash;
    /** Original name of the file.*/
    private String _blobName;

}
