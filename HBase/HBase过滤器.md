#### 过滤器

HBase过滤器提供强大的特性帮助用户提高表处理的效率.用户既可以使用HBase预定义好的过滤器,也可以自定义过滤器.

HBase API中使用`get()`或者`scan()`方法读取数据,可以通过添加列族,列,时间戳以及版本号信息进行限制查询.但是缺陷就是查询的粒度粗,不支持正则表达式以及前缀查询等等.

用户可以在客户端通过继承`Filter`类实现自己的需求.所有的过滤器都在服务端生效,叫做谓词下推.这样可以保证过滤掉的数据不会被传送到服务端.用户可以在客户端实现过滤的功能.(会导致性能的下降).

过滤器底层提供的是`Filter`接口以及`FilterBase`抽象类,实现了过滤器的框架.在实例化的过程中,需要设定一些参数指定过滤器的功能.其中比较类型的过滤器`CompareFilter`需要用户提供**运算符**以及**比较器**.

##### 运算符类型

| 运算符           | 描述       |
| ---------------- | ---------- |
| LESS             | 小于       |
| LESS_OR_EQUAL    | 小于等于   |
| EQUAL            | 等于       |
| NOT_EQUAL        | 不等于     |
| GREATER_OR_EQUAL | 大于等于   |
| GREATER          | 大于       |
| NO_OP            | 排除一切值 |

##### 比较器类型

| 比较器类型             | 描述                            |
| ---------------------- | ------------------------------- |
| BinaryComparator       | 值比较                          |
| BinaryPrefixComparator | 前缀比较                        |
| NullComparator         | 不做比较,仅仅判断当前是不是null |
| BitComparator          | 按位与,或,异或操作              |
| RegexComparator        | 按照正则规则匹配                |
| SubstringComparator    | 子串匹配                        |

##### 比较过滤器

**行过滤器**

```java
Scan scan=new Scan();
scan.addColumn(Bytes.toBytes("column1"),Bytes.toBytes("col-0"));
Filter filter1=new RowFilter(CompareFilter.CompareOp.LESS_OR_EQUAL,
                           new BinaryComparator(Bytes.toBytes("row-22")));
scan.setFilter(filter1);
ResultScanner scaner=table.getScanner(scan);
for(Result res:scaner){
    System.out.println(res);
}
scaner.close();
```

上述是一个行过滤器，采用了值比较的方式。

**列族过滤器**

```java
Scan scan=new Scan();
scan.addColumn(Bytes.toBytes("column1"),Bytes.toBytes("col-0"));
Filter filter1=new FamilyFilter(CompareFilter.CompareOp.LESS_OR_EQUAL,
                           new BinaryComparator(Bytes.toBytes("row-22")));
scan.setFilter(filter1);
ResultScanner scaner=table.getScanner(scan);
for(Result res:scaner){
    System.out.println(res);
}
scaner.close();
```

**列过滤器**

```java
Scan scan=new Scan();
scan.addColumn(Bytes.toBytes("column1"),Bytes.toBytes("col-0"));
Filter filter1=new QualifierFilter(CompareFilter.CompareOp.LESS_OR_EQUAL,
                           new BinaryComparator(Bytes.toBytes("row-22")));
scan.setFilter(filter1);
ResultScanner scaner=table.getScanner(scan);
for(Result res:scaner){
    System.out.println(res);
}
scaner.close();
```

**值过滤器**

```java
Scan scan=new Scan();
scan.addColumn(Bytes.toBytes("column1"),Bytes.toBytes("col-0"));
Filter filter1=new ValueFilter(CompareFilter.CompareOp.LESS_OR_EQUAL,
                           new BinaryComparator(Bytes.toBytes("row-22")));
scan.setFilter(filter1);
ResultScanner scaner=table.getScanner(scan);
for(Result res:scaner){
    System.out.println(res);
}
scaner.close();
```

##### 专用过滤器

1. 单列值过滤器(SingleColumnValueFilter)
2. 单列值排除过滤器(SingleColumnValueExcludeFilter)
3. 前缀过滤器(PrefixFilter)
4. 分页过滤器(PageFilter)
5. 行键过滤器(KeyOnlyFilter)
6. 首次行键过滤器(FirstKeyOnlyFilter)
7. 时间戳过滤器(TimestampFilter)
8. 列计数过滤器(ColumnCountGetFilter)
9. 列分页过滤器(ColumnPaginationFilter)
10. 列前缀过滤器(ColumnPrefixFilter)
11. 随机行过滤器(RandomRowFilter)

实际应用中，用户需要多个过滤器共同限制返回到客户端的结果，`FieldList`提供了这个功能。

#### 计数器

许多收集统计信息的应用，会收集日志文件中的信息，以便于后续分析。用户可以利用计数器进行实时统计。

##### 单计数器

这种计数器需要用户自己设定列

```java
HTable table=new HTable(conf,"counterts");
long cnt1=table.incrementColumnValue(Bytes.toBytes("20110101")),
	Bytes.toBytes("daily"),Bytes.Bytes("hits",1);
```

##### 多计数器

多计数器需要用户创建一个Increment实例，同时需要填充一些细节到实例中。

```java
Increment increment=new Increment(Bytes.toBytes("20110101"));
increment.addColumn(Bytes.toBytes("daily"),Bytes.toBytes("clicks"),1);
increment.addColumn(Bytes.toBytes("daily"),Bytes.toBytes("hits"),1);
increment.addColumn(Bytes.toBytes("weekly"),Bytes.toBytes("clicks"),10);
increment.addColumn(Bytes.toBytes("weekly"),Bytes.toBytes("hits"),1);

Result result=table.increment(increment);
for(KeyValue kv: result.raw()){
    System.out.println("Kv: "+kv+"\tvalue "+Bytes.toLong(kv.getValue()));
}
```

#### 协处理器

HBase提供一些将计算移动到数据的存放处，这个就是**协处理器(coprocessor)**

协处理器允许用户在region服务器上运行自己的代码，准确地说允许用户执行region级别的操作。协处理器提供了`Observer`和`Endpoint`两个部分支持扩展功能。

其中`Observer`提供region周期中的回调函数功能。其中包含有：

1. `RegionObserver`

   用于控制region的生命周期中的回调处理

2. `MasterObserver`

   监控Master生命周期，集群性时间

3. `WALObserver`

   WAL的回调函数

`Endpoint`用户存储一些用户的代码，同时需要进行事件的处理，并通过远程调用在远程执行用户代码。

#### HTablePool

与其为每个客户端都创建一个HTable实例，不如创建一个实例，然后不断复用。所以这里使用了连接池的方法。

```java
Configuration conf=HBaseConfiguration.create();
HTablePool pool=new HTablePool(conf,Integer.MAX_VALUE);
```

