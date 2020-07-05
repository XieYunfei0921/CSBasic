#### 压缩

HBase支持大量的压缩算法，并且可以支持列族级别以上的数据压缩，通常情况下压缩可以带来较好的性能。

#### 常用的压缩算法性能对比

| 算法名称 | 压缩比(%) | 压缩     | 解压     |
| -------- | --------- | -------- | -------- |
| GZIP     | 13.4      | 21 M/s   | 118 MB/s |
| LZO      | 20.5      | 135 MB/s | 410 MB/s |
| Snappy   | 22.2      | 172 MB/s | 409 MB/s |

#### 拆分和合并的优化

HBase通常是自动拆分region的，一旦达到既定的阈值，region就会被拆分成两个，之后可以继续接受数据，并保持增长。但是可能会出现**拆分/合并风暴**.

> 当region大小以恒定速度保持增长的时候,region拆分会在同时进行.因为需要压缩region中的存储文件,这个过程会重新拆分之后的region.会引起IO的上升.

可以手动运行命令`split`,`major_compact`进行合并,可以在不同的region上交错进行.可以分散IO负载.

##### Region热点

由于数据的特征会出现数据热点的问题，会让某些region访问的频次特别高。所以常常采用**盐析主键**的方式进行处理。或者使用随机行键进行负载均衡处理。

##### 预拆分Region

管理接口中`createTable()`方法和Shell中的`create`命令都可以接受以列表形式提供的拆分行键作为参数，该参数在创建表的时候用于拆分region。HBase给用户提供一个用于拆分region的类`RegionSplitter`.默认采样MD5进行预拆分。当然可以自己指定拆分的算法。

```shell
$ ./bin/hbase org.apache.hadoop.hbase.util.RegionSplitter -c 10 testtable -f colfam1
# 或者使用hbase shell指令
hbase(main):001:0 > create 'testtable','colfam1',{ \
	SPLITS => ['row-100','row-200','row-300','row-400']} 
```

关于如何设置region的数量，可以预先设置10个region来进行预拆分。随着时间的增长查看数据的增长情况。先设置较少的region数目，然后再滚动拆分是一种较好的方法。

#### 负载均衡

master中有一个均衡器的功能，每5 min执行一次，通过`hbase-balancer-period`属性设置。一旦均衡器启动，就会尝试将region均衡的分配到所有的region服务器中。

