#### 介绍
基于hash表实现的Map接口,允许key/value为空,hashMap大体上与hashTable类似,但是是非同步的请允许空值.这个类不保证map的顺序.基本操作`put`和`get` 是常数时间复杂度的操作,初始化容量和加载因子不应设置的过大.

hashMap实例有两个影响性能的参数,分别是**初始容量**和**加载因子**.容量是hash表的槽数量，初始化容量是hash表创建的时候的容量.加载因子是hash表处于什么位置时允许扩容。当hash表的条目数量超出加载因子的容量，hash表会进行rehash操作，即对内部数据进行重新构建，这样hash表的容量可以翻倍。

默认的加载因子0.75提供了对时间和空间的良好交换效果，这个值大的会会降低空间开销但是会增加查询消耗。当设置初始容量是需要考虑到加载因子，和预期map的大小。如果初始容量大于加载因子界定的条目的最大数量，就不会触rehash操作.
如果有大量数据存储在hashmap中，大容量创建要由于不停的扩容的方式。注意到多个key使用相同的hashcode函数会降低hash表的性能。注意到这个实现不是同步的，如果多个线程并发访问，且至少一个线程修改了这个map，就必须进行外部同步。

如果不使用外部同步就需要使用`synchronizedMap`,例如:

```java
Map m = Collections.synchronizedMap(new HashMap(...));
```


这个类的迭代器具有快速失败的特性，在并发修改的时候，迭代器会快速失败，不会去冒险执行。
这里的快速是啊比并非完全的快速失败，只是尽最大努力的快速失败。因此快速失败用于检测bug

##### 实现要点

```markdown
这个map用作桶装的hash表，但是当hash桶变得过大的时候，会转换为树形节点，每个结构与TreeMap类似。

大多数方法尝试获取正常的hash桶。树节点与其他类型节点遍历类型，但是支持快速查找。树形节点提供了O(log n)的查询时间复杂度.由于树形节点大小是平常节点的两倍，所以只有在节点足够多的时候才会使用。

且当其数量变少的时候需要将其转换为普通节点。在散列情况良好的map中很少需要使用树形节点。理想情况下，使用随机hashcode的情况下，槽中节点的数量遵循泊松分布。

树形节点的根节点通常情况下是首个节点，但是有的时候（例如，移除了某个元素）。这个根节点会位于其他位置，但是可以通过拓扑关系进行恢复。所有的内部方法都会接受hash code作为参数，使得可以不需要重新计算hashcode的进行互相调用。

大多数内部方法同时接受一个tab参数，正常情况下是当前的table，但是resize或者转换的时候会变成新的值。当hash槽中的元素树形化，解除树形化之后，保证相应的获取顺序。
```

#### 属性

```markdown
1. DEFAULT_INITIAL_CAPACITY=1<<4
默认初始容量,16,设定的值需要是2的n次方
2. MAXIMUM_CAPACITY = 1 << 30
容量最大值 2^30,设定值需要小于等于2^30
3. DEFAULT_LOAD_FACTOR = 0.75f
加载因子,默认0.75
4. TREEIFY_THRESHOLD = 8
hash槽元素树结构化最小元素数量
5. UNTREEIFY_THRESHOLD = 6
hash槽元素解除树结构化元素数量
6. MIN_TREEIFY_CAPACITY
hash树结构化最小hash表容量,这个值至少要大于4 * TREEIFY_THRESHOLD
7. Node<K,V>[] table
hash树结构化最小hash表容量,这个值至少要大于4 * TREEIFY_THRESHOLD
8. Set<Map.Entry<K,V>> entrySet
持有缓存的@entrySet(),注意@AbstractMap用于@keySet() @values()
9. int modCount
hashmap以及修改的次数.这个属性用于使得迭代器具有快速失败的功能.
10. int threshold
下次扩容的长度
11. loadFactor
hash表的加载因子
```

#### 常用方法

##### void treeifyBin(Node<K,V>[] tab, int hash)

将链表节点转换为树状节点

```java
final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            TreeNode<K,V> hd = null, tl = null;
            do {
                TreeNode<K,V> p = replacementTreeNode(e, null);
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            if ((tab[index] = hd) != null)
                hd.treeify(tab);
        }
    }
```

##### boolean containsValue(Object value) 

返回true表示,map中包含一个或者多个对应这个value的key

##### Set<K> keySet()

返回这个map的key集合,修改map会影响这个key集合,反之亦然.集合支持元素的移除操作,不支持@add @addAll方法

##### Collection<V> values()

返回当前map的value，map的改变会反应到这个集合中，反之亦然。支持移除操作，不支持添加操作。

---

JDK8 新方法

##### V computeIfAbsent(K key,Function<? super K, ? extends V> mappingFunction)

如果发现map函数修改了这个map会抛出异常

```java
if (mappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        // 值存在直接返回旧值
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
            V oldValue;
            if (old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        // 值不存在使用函数计算并替换
        int mc = modCount;
        // 应用映射函数获取value值
        V v = mappingFunction.apply(key);
        // 如果发生了并发修改,抛出异常
        if (mc != modCount) { throw new ConcurrentModificationException(); }
        if (v == null) {
            return null;
        } else if (old != null) {
            // 修改value值
            old.value = v;
            afterNodeAccess(old);
            return v;
        }
        else if (t != null) // 替换树节点
            t.putTreeVal(this, tab, hash, key, v);
        else {
            // 新增节点,并插入到树中
            tab[i] = newNode(hash, key, v, first);
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        modCount = mc + 1;
        ++size;
        afterNodeInsertion(true);
        return v;
```

##### V merge(K key, V value,BiFunction<? super V, ? super V, ? extends V> remappingFunction)

合并新旧值

```java
public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null)
            throw new NullPointerException();
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        if (old != null) {
            V v;
            if (old.value != null) {
                int mc = modCount;
                v = remappingFunction.apply(old.value, value);
                if (mc != modCount) {
                    throw new ConcurrentModificationException();
                }
            } else {
                v = value;
            }
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            }
            else
                removeNode(hash, key, null, false, true);
            return v;
        }
        if (value != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, value);
            else {
                tab[i] = newNode(hash, key, value, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return value;
    }
```

##### void forEach(BiConsumer<? super K, ? super V> action)

map的每个元素迭代进行指定的函数运算

```java
public void forEach(BiConsumer<? super K, ? super V> action) {
    Node<K,V>[] tab;
    if (action == null)
        throw new NullPointerException();
    if (size > 0 && (tab = table) != null) {
        int mc = modCount;
        for (Node<K,V> e : tab) {
            for (; e != null; e = e.next)
                action.accept(e.key, e.value);
        }
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }
}
```

##### void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)

使用指定函数对value进行全部替换

```java
public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    Node<K,V>[] tab;
    if (function == null)
        throw new NullPointerException();
    if (size > 0 && (tab = table) != null) {
        int mc = modCount;
        for (Node<K,V> e : tab) {
            for (; e != null; e = e.next) {
                e.value = function.apply(e.key, e.value);
            }
        }
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }
}
```

#### HashSet

##### 介绍

```markdown
Set接口的实现,对@add @remove @contains @size方法的常数时间的复杂度.假定hash函数可以很好的对元素进行散列.不需要设置初始容量过程(或者设置加载因子太小).注意到这个实现不是同步的，如果多线程并发获取一个hash set,且至少一个线程修改了这个集合,必须在外部进行同步.可以对对象加锁来进行同步.
如果不使用对象加锁的方式就需要使用相应的包装类@synchronizedSet
Set s = Collections.synchronizedSet(new HashSet(...));
这个类的迭代器具有快速失败的特性,如果集合创建之后,如果堆迭代器进行并发修改,迭代器会快速失败并情况,而非是冒险尝试.注意到不能保证迭代器的完全快速失败,通常来说是按照最大努力保证快速失败.因此,快速失败会用于来发现Bug.
```

##### 常用方法

###### public HashSet()

构建新的,空的set结合,返回的hashMap默认容量为16,加载因子为0.75

```java
public HashSet() {map = new HashMap<>();}
```

###### Iterator<E> iterator()

返回当前集合的迭代器

```java
public Iterator<E> iterator() {
    return map.keySet().iterator();
}
```

###### void writeObject(java.io.ObjectOutputStream s)

序列化当前集合类
1. 写出序列化魔数
2. 写出容量信息
3. 写出加载因子信息
4. 写出集合大小信息
5. 写出值信息