TODO NormalizedPathFingerprintCompareStrategy 没看明白

CompositeFileCollection
 DefaultFileCollectionResolveContext
  FileTreeAdapter
   FileSystemMirroringFileTree
    ArchiveFileTree
    TarFileTree
    ZipFileTree
   AbstractFileCollection
    RandomAccessFileCollection
    GeneratedSingletonFileTree
    
ModuleVersionResolveException    

DefaultFileSystemSnapshotter
 DirectorySnapshotter
  MerkleDirectorySnapshotBuilder
   RelativePathSegmentsTracker
 FileSystemSnapshotBuilder

### FileTreeAdapter
适配 
- FileSystemMirroringFileTree . tar/zip? 对应的 tree
- LocalFileTree 实实在在的本地目录 对应 tree

### GeneratedSingletonFileTree

使用文件时 会根据情况选择用 contentWriter 动态写内容到文件

### FileCollectionInternal

## LegacyTypesSupport

TODO 字节码修改？



