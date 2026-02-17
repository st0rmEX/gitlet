package gitlet;

import java.io.File;
import java.util.Date;

/** Driver class for Git-lite
 *  @author Troy Burad
 */
public class Main {
    /** Current working directory file shortcut.*/
    static final File CWD = new File(System.getProperty("user.dir"));
    /** .gitlet directory shortcut.*/
    static final File DOTGITLET = Utils.join(CWD, ".gitlet");
    /** Branch pointer shortcut.*/
    static final File BRANCHES = Utils.join(DOTGITLET, "branches");
    /** Blob pointer shortcut.*/
    static final File BLOBS = Utils.join(DOTGITLET, "blobs");
    /** Commits directory.*/
    static final File COMMITS = Utils.join(DOTGITLET, "commits");
    /** Points to head branch.*/
    static final File HEAD = Utils.join(DOTGITLET, "HEAD");
    /** File structure storing info on file removal and addition.*/
    static final File STAGE = Utils.join(DOTGITLET, "stage");
    /** Tracks files added.*/
    static final File STAGEADD = Utils.join(STAGE, "add");
    /** Stores the same files as from previous commits unchanged.*/
    static final File STAGESAME = Utils.join(STAGE, "same");
    /** Tracks removed files.*/
    static final File STAGEREMOVE = Utils.join(STAGE, "remove");
    /** Shortcuts.*/
    static final File SHORTCUTS = Utils.join(DOTGITLET, "shortcuts");
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        Date currentDate = new Date();
        try {
            if (args.length == 0) {
                throw new GitletException("Please enter a command.");
            }
            if (args[0].equals("init")) {
                argLengthEquals(1, args);
                DotGitlet.initGitlet();
            } else if (!DOTGITLET.isDirectory()) {
                throw new GitletException("Not in an "
                        + "initialized Gitlet directory.");
            } else if (args[0].equals("add")) {
                argLengthEquals(2, args);
                Stage.add(args[1]);
            } else if (args[0].equals("commit")) {
                argLengthEquals(2, args);
                Stage.makeCommit(args[1],
                        String.valueOf(currentDate.getTime()));
            } else if (args[0].equals("rm")) {
                argLengthEquals(2, args);
                Stage.remove(args[1]);
            } else if (args[0].equals("log")) {
                DotGitlet.log();
            } else if (args[0].equals("global-log")) {
                DotGitlet.globalLog();
            } else if (args[0].equals("find")) {
                DotGitlet.find(args[1]);
            } else if (args[0].equals("status")) {
                DotGitlet.status();
            } else if (args[0].equals("checkout")) {
                if (args[1].equals("--")) {
                    Stage.checkout1(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    Stage.checkout2(args[1], args[3]);
                } else if (args.length == 2) {
                    Stage.checkout3(args[1]);
                } else {
                    throw new GitletException("Incorrect operands.");
                }
            } else if (args[0].equals("branch")) {
                argLengthEquals(2, args);
                Stage.createBranch(args[1]);
            } else if (args[0].equals("rm-branch")) {
                argLengthEquals(2, args);
                Stage.rmBranch(args[1]);
            } else if (args[0].equals("reset")) {
                argLengthEquals(2, args);
                Stage.reset(args[1]);
            } else if (args[0].equals("merge")) {
                argLengthEquals(2, args);
                Stage.merge(args[1]);
            } else {
                throw new GitletException("No command with that name exists.");
            }
        } catch (GitletException a) {
            System.out.println(a.getMessage());
            System.exit(0);
        }
    }

    private static void argLengthEquals(int requiredArgs, String[] args) {
        if (args.length != requiredArgs) {
            throw new GitletException("Incorrect operands.");
        }
    }
}
