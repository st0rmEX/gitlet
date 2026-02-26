# Gitlet
A lite-version of the widely used version-control system Git. In this implementation, Gitlet uses tree like data structures to store "blobs" of 
information containing file contents. Commits and switching between different branches are supported.
## Supported Commands
### - `init`
```
java gitlet.Main init
```
Creates a new Gitlet version-control system in the current directory.
### - `add`
```
java gitlet.Main add [file name]
```
Adds a copy of the file as it currently exists to the staging area.
### - `commit`
```
java gitlet.Main commit [message]
```
Saves a snapshot of tracked files in the current commit and staging area so they can be restored at a later time, creating a new commit.
### - `rm`
```
java gitlet.Main rm [file name]
```
Unstage the file if it is currently staged for addition. If the file is tracked in the current commit, stage it for removal and remove the file from the working directory.
### - `log`
```
java gitlet.Main log
```
Starting at the current head commit, display information about each commit backwards along the commit tree until the initial commit.
### - `global-log`
```
java gitlet.Main global-log
```
Displays information about all commits ever made
### - `find`
```
java gitlet.Main find [commit message]
```
Prints out the ids of all commits that have the given commit message.
### - `status`
```
java gitlet.Main status
```
Displays what branches currently exist, and marks the current branch with a *. Also displays what files have been staged for addition or removal
### - `checkout`
```
1. java gitlet.Main checkout -- [file name]
2. java gitlet.Main checkout [commit id] -- [file name]
3. java gitlet.Main checkout [branch name]
```
1. Takes the version of the file as it exists in the head commit and puts it in the working directory.
2. Takes the version of the file as it exists in the commit with the given id and puts it in the working directory.
3. Takes all files in the commit at the head of the given branch and puts them in the working directory, overwriting the versions of the files that are already there if they exist.
### - `branch`
```
java gitlet.Main branch [branch name]
```
Creates a new branch with the given name, and points it at the current head node. Default branch is "master".
### - `rm-branch`
```
java gitlet.Main rm-branch [branch name]
```
Deletes the branch with the given name.
### - `merge`
```
java gitlet.Main merge [branch name]
```
Merges files from the given branch into the current branch.
### - `reset`
```
java gitlet.Main reset [commit id]
```
Checks out all the files tracked by the given commit. Removes tracked files that are not present in that commit.
