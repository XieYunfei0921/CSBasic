#### 介绍

```markdown
双向链表，同时实现了列表和队列。实现所有列表的操作，允许所有元素为空.对索引的操作会遍历整个链表。注意这个实现不是同步的，如果多个线程并发获取linklist，并且至少有一个线程修改了链表。那么必须要进行外部同步。否则需要使用@synchronizedList对其进行包装。迭代器创建之后的链表数据结构修正，会抛出@ConcurrentModificationException异常。在并发修改的情况下，迭代器会快速失败，不会冒险去执行。
```

#### 常用方法介绍

##### E unlinkFirst(Node<E> f) 

解除首个非空节点的链接

```java
E unlinkFirst(Node<E> f) {
    // assert f == first && f != null;
    final E element = f.item;
    final Node<E> next = f.next;
    f.item = null;
    f.next = null; // help GC
    first = next;
    if (next == null)
        last = null;
    else
        next.prev = null;
    size--;
    modCount++;
    return element;
}
```

##### E unlinkLast(Node<E> l

解除最后一个结束节点

```java
E unlinkLast(Node<E> l) {
    // assert l == last && l != null;
    final E element = l.item;
    final Node<E> prev = l.prev;
    l.item = null;
    l.prev = null; // help GC
    last = prev;
    if (prev == null)
        first = null;
    else
        prev.next = null;
    size--;
    modCount++;
    return element;
}
```

##### boolean contains(Object o)

确定是否包含某个节点

##### E get(int index)

获取指定索引的元素

##### E set(int index, E element)

设置指定索引的元素

##### void add(int index, E element) 

在指定索引处添加指定元素

##### int indexOf(Object o)

获取第一次出现元素o的位置

##### E peek()

检索但是不会移除列表的首部，如果为空则返回null

##### E poll()

检索并移除链表的首部

##### boolean offer(E e)

添加指定的元素到列表末尾

##### boolean offerFirst(E e)

插入指定元素到列表头部

#### LinkedHashMap

```markdown
Map接口的hash表和链表实现，使用可预测的迭代顺序。实现不同于hashmap的是其维护了一个双向链表.注意到当一个key重新插入到链表的时候.顺序不受影响.
```

