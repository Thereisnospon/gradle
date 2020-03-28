

``` 
a1
 b1
  c1
  c2.txt
 b2
  c2
   d1.txt
  c1.txt
 b4
  c1.txt
 b3.txt
```

输出 

``` 
a1
a1/b1
a1/b1/c1
a1/b1/c2.txt
a1/b2
a1/b2/c1.txt
a1/b2/c2
a1/b2/c2/d1.txt
a1/b3.txt
a1/b4
a1/b4/c1.txt


```

FileVisitResult.SKIP_SUBTREE

- preVisitDirectory ,忽略 子文件子目录 并且不会调用该目录的 postVisitDirectory
- postVisitDirectory, 无效

FileVisitResult.SKIP_SIBLINGS

- preVisitDirectory ,忽略后面的兄弟节点 以及子目录.并且不会调用该目录的 postVisitDirectory
- postVisitDirectory, 无效
