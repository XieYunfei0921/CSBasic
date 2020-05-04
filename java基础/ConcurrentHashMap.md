#### 介绍

```markdown
支持全并发检索和高期望并发更新的hash表.这个类支持hash表.但是,尽管所有的操作都是线程安全的,检索操作需要锁定操作,且不支持对整个hash表进行锁定.这个类是与hash表相互操作.检索操作@get 不会阻塞,所以可以与更新操作进行重叠.检索操作反应了最近完成的结果.对于聚合操作,例如putAll或者clear方法,并发检索仅仅会反应一条数据的增删结果.这个方法不会抛出并发修改异常,但是这个类不会抛出并发修改异常.但是,迭代器一个时刻只能被一个线程使用.如果存在太多的hash碰撞的时候,这个表可以动态扩展,但是表的动态扩展是一个相当慢的操作.可能的话,使用@initialCapacity对表的大小做出估计是有必要的.另一个需要的参数就是加载因子.此外,对于上一个状态的容量,构造器可以指定一个并发等级@concurrencyLevel 作为内部大小设置的线索.注意到如果多个相同的key会降低hash表的时间复杂度. @ConcurrentHashMap 的key存储在一个集合中.同时@ConcurrentHashMap 可以作为可扩展的map,通过使用原子包的@LongAdder 实现,且通过方法@computeIfAbsent 进行初始化.
例如,添加一个计数值到@ConcurrentHashMap<String,LongAdder> freqs 中,可以使用:
	freqs.computeIfAbsent(key, k -> new LongAdder()).increment();进行扩展
这个类以及其迭代器实现了所有map接口的方法.这个类类似于hashtable但是与hashmap类不相似,不允许空值作为key/value
这个类支持序列集合和并行的批量操作,与@Stream方法不同的是,这个类是安全的,可以被线程并发的修改.例如,当计算共享注册表中的副本描述的时候.有三种操作,每个有4个参数,接受key/value,条目信息或者kv对作为参数.由于ConcurrentHashMap的元素没有经过排序.且可以在不同的并行执行器中按照不同的顺序进行处理.提供函数的正确性不能依靠于任何的顺序,或者是其他在计算过程中可变的对象或者值.(处理forEach方法),因为这个方法具有边界效应免疫的特性.
常用操作:
foreach:
	对于每个元素进行操作,在每个元素上使用给定的转换.
search:
	对每个元素应用给定的函数，返回首个非空的结果值
reduce:
	累加每个元素,提供的reduce函数不能依靠于排序
这里有5个参数:
1. 普通reduce
2. map reduce,对于每个元素使用给定的函数，计算reduce的结果
3. 使用给定的基础值，对double,long,int进行reduce操作
批量操作会接受一个@parallelismThreshold 参数.如果当前map大小小于这个容量,使用Long.MAX_VALUE可以抑制所有并行度。
批量操作的并发属性遵守着如下条件:
任何get方法返回的非空值,会与相关的插入/更新操作相关.批量操作的结果反映了每个元素的关系.相反地,由于key和value不会是null值.空值可以任何当前值缺失的结果.为了维护这个属性,null值可以作为非标量reduce操作的显示基础值.查找和转换功能提供了相似的参数,返回空值则表示结果的缺失.在map reduce中,可以开启过滤器,如果元素不能合并的时候则会返回null.方法接受或者返回的条目参数可以维护kv关系.批量操作可以突然停止,并抛出异常.处理这些异常的时候,其他并发执行函数也可以抛出这样的异常,如果首个异常没有发生的时候就可能已经发生完毕了.
```

#### 属性

部分属性

```markdown
1. ObjectStreamField[] serialPersistentFields
序列化的属性值,用于兼容jdk 7
2. class Node<K,V> implements Map.Entry<K,V>
kv属性值,可以在批量任务中作为只读变量.
3. Node<K,V>[] nextTable
下一个需要使用的表,只有当改变大小的时候为非空
4. long baseCount
基本计数值,没有竞争的时候使用,但是在初始化竞争的时候作为回调,使用CAS更新
5. int transferIndex
当改变大小的时候下一个表的位置
6. int cellsBusy
自旋锁,通过CAS进行加锁,改变大小或者创建计数单元的时候使用
7. CounterCell[] counterCells
计数器存储单位表,非空,大小为2的n次方
```


#### 原子性操作方法

##### Node<K,V> tabAt(Node<K,V>[] tab, int i)

原子性获取指定索引出的节点信息

```java
static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
	return (Node<K,V>)U.getObjectAcquire(tab, ((long)i << ASHIFT) + ABASE);
}
```

##### boolean casTabAt(Node<K,V>[] tab, int i,Node<K,V> c, Node<K,V> v)

cas更新指定节点

```java
static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
	Node<K,V> c, Node<K,V> v) {
	return U.compareAndSetObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
}
```

##### void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v)

设置指定节点

```java
static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
    U.putObjectRelease(tab, ((long)i << ASHIFT) + ABASE, v);
}
```

#### 常用方法

##### V put(K key, V value)

映射指定key和指定value,kv都不能为空,value可以通过get方法进行检索
##### void putAll(Map<? extends K, ? extends V> m)
将指定map中的映射全部拷贝.
##### V remove(Object key)
从map中移除key,如果key不存在,那么什么都不会处理
##### void clear()
清理map中的所有映射
##### void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab)
移动或者拷贝节点到新的hash表中

##### V putVal(K key, V value, boolean onlyIfAbsent)
存储指定的kv值
```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh; K fk; V fv;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value)))
                    break;                   // no lock when adding to empty bin
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else if (onlyIfAbsent // check first node without acquiring lock
                     && fh == hash
                     && ((fk = f.key) == key || (fk != null && key.equals(fk)))
                     && (fv = f.val) != null)
                return fv;
            else {
                V oldVal = null;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key, value);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                        else if (f instanceof ReservationNode)
                            throw new IllegalStateException("Recursive update");
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }
```