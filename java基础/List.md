### ArrayList

#### 介绍

```markdown
List的实现,大小可变的数组.实现所有list的操作.处理实现List接口之外,这个类提供了操作数组大小的功能.其中@size @isEmpth @get @set @iterator和@listIterator运行时间为常数等级,添加操作也是常数的时间复杂度.其他操作都是线性时间复杂度.相比较LinkedList来说具有较低的常数因子.每个@ArrayList对象都有一个@capacity设置,这个是用于存储list元素的数组大小,至少要比实际数组大.当元素插入到ArrayLost的时候,会自动进行扩容.一个应用在插入大量元素的时候,需要使用@ensureCapacity检查是否具有足够的容量.会降低重分配的数量.
注意到这个实现是非同步的.如果多个线程获取ArrayList对象,且至少一个线程修改了list,那么必须要使用外部同步措施.典型的实现是在一些对象上使用同步措施,这样可以将元素有序压入列表中.如果不存在这样的对象,可以使用这个列表的保证类@synchronizedList,按照如下方法定义
	List list = Collections.synchronizedList(new ArrayList(...));
快速失败
这个类@iterator()返回的迭代器以及@listIterator(int)可以快速失败.如果列表进行了结构化的修改,迭代器会抛出一个@ConcurrentModificationException异常,在并发修改的情况下,迭代器会快速失败,而非是冒险执行.
注意到迭代器的快速失败行为不能够完全包住并发修改的不存在性,抛出的@ConcurrentModificationException是尽最大努力做到的.因此,快速失败的行为仅仅用于发现bug
```

#### 属性

```markdown
1. DEFAULT_CAPACITY = 10
默认容量
2. Object[] EMPTY_ELEMENTDATA
共享的空数组对象,用于给空实例的
3. Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA
共享的空数组对象，用于默认长度的空实例，与@EMPTY_ELEMENTDATA 不同的是可以知道当第一个元素添加的时候需要扩展多少
4. Object[] elementData
ArrayList元素的数组缓冲区,容量是这个数组的缓冲长度,空数组首次扩展的容量为默认容量
5. MAX_ARRAY_SIZE= Integer.MAX_VALUE - 8
ArrayList最大分配长度,因为一些虚拟机有header worker所以值为MAX_VALUE - 8
```

#### 常用方法

##### ArrayList(int initialCapacity)

指定初始容量,构建空列表

```java
public ArrayList(int initialCapacity) {
    if (initialCapacity > 0) {
        this.elementData = new Object[initialCapacity];
    } else if (initialCapacity == 0) {
        this.elementData = EMPTY_ELEMENTDATA;
    } else {
        throw new IllegalArgumentException("Illegal Capacity: "+
                                           initialCapacity);
    }
}
```

##### public ArrayList(Collection<? extends E> c)

由指定集合构建列表

```java
public ArrayList(Collection<? extends E> c) {
    elementData = c.toArray();
    if ((size = elementData.length) != 0) {
        // defend against c.toArray (incorrectly) not returning Object[]
        // (see e.g. https://bugs.openjdk.java.net/browse/JDK-6260652)
        if (elementData.getClass() != Object[].class)
            elementData = Arrays.copyOf(elementData, size, Object[].class);
    } else {
        // replace with empty array.
        this.elementData = EMPTY_ELEMENTDATA;
    }
}
```

##### public void trimToSize() {

将容量减少到与列表长度一致

##### void ensureCapacity(int minCapacity)

ArrayList扩容

##### Object clone()

返回当前对象的浅拷贝实例,元素自身没有被拷贝

##### E get(int index)

返回指定位置的元素

##### boolean add(E e)

添加元素到列表的末尾
##### void add(int index, E element)
插入元素到指定位置,需要移动这个位置原来的元素以及右边的元素向右移动
##### E remove(int index)
移除指定位置元素,右边的元素需要向左移动一个位置
##### void fastRemove(Object[] es, int i) 
快速移动,不会返回移除的元素值且不会进行界限检查
##### boolean addAll(Collection<? extends E> c)
快速移动,不会返回移除的元素值且不会进行界限检查
```java
public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        modCount++;
        int numNew = a.length;
        if (numNew == 0)
            return false;
        Object[] elementData;
        final int s;
        if (numNew > (elementData = this.elementData).length - (s = size))
            elementData = grow(s + numNew);
        System.arraycopy(a, 0, elementData, s, numNew);
        size = s + numNew;
        return true;
}
```
##### void writeObject(java.io.ObjectOutputStream s)
序列化,写出的是列表元素,然后序列化列表长度
##### void readObject(java.io.ObjectInputStream s)
反序列化
