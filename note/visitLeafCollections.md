
```java
public interface FileCollectionInternal extends FileCollection {
    
    void registerWatchPoints(FileSystemSubset.Builder builder);
    //在一个 FileCollection 的层级中访问某个层级的 叶子节点。（层次遍历某一层？然后 visitor 选择访问下一层？)
    void visitLeafCollections(FileCollectionLeafVisitor visitor);
}
public abstract class AbstractFileCollection implements FileCollectionInternal {
    @Override
    public void visitLeafCollections(FileCollectionLeafVisitor visitor) {
        visitor.visitCollection(this);
    }    
}
public abstract class AbstractFileTree extends AbstractFileCollection implements FileTreeInternal {
    @Override
    public void visitLeafCollections(FileCollectionLeafVisitor visitor) {
        visitor.visitGenericFileTree(this);
    }    
}
public abstract class CompositeFileCollection extends AbstractFileCollection implements FileCollectionContainer, TaskDependencyContainer {
    @Override
    public void visitLeafCollections(FileCollectionLeafVisitor visitor) {
        for (FileCollectionInternal element : getSourceCollections()) {
            element.visitLeafCollections(visitor);
        }
    }
}
public class FileTreeAdapter extends AbstractFileTree implements FileCollectionContainer {
    @Override
    public void visitLeafCollections(FileCollectionLeafVisitor visitor) {
        if (tree instanceof DirectoryFileTree) {
            DirectoryFileTree directoryFileTree = (DirectoryFileTree) tree;
            visitor.visitFileTree(directoryFileTree.getDir(), directoryFileTree.getPatterns());
        } else if (tree instanceof SingletonFileTree) {
            SingletonFileTree singletonFileTree = (SingletonFileTree) tree;
            visitor.visitFileTree(singletonFileTree.getFile(), singletonFileTree.getPatterns());
        } else if (tree instanceof ArchiveFileTree) {
            ArchiveFileTree archiveFileTree = (ArchiveFileTree) tree;
            File backingFile = archiveFileTree.getBackingFile();
            if (backingFile != null) {
                visitor.visitCollection(ImmutableFileCollection.of(backingFile));
            } else {
                visitor.visitGenericFileTree(this);
            }
        } else {
            visitor.visitGenericFileTree(this);
        }
    }    
}
```

```java
private class FileCollectionLeafVisitorImpl implements FileCollectionLeafVisitor {
    private final List<FileSystemSnapshot> roots = new ArrayList<FileSystemSnapshot>();
    @Override
    public void visitCollection(FileCollectionInternal fileCollection) {
        for (File file : fileCollection) {
            roots.add(snapshot(file));
        }
    }
    @Override
    public void visitGenericFileTree(FileTreeInternal fileTree) {
        roots.add(snapshotFileTree(fileTree));
    }
    @Override
    public void visitFileTree(File root, PatternSet patterns) {
        roots.add(snapshotDirectoryTree(root, patterns));
    }
    public List<FileSystemSnapshot> getRoots() {
        return roots;
    }
}
```
