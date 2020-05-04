#### ThreadLocal介绍

```markdown
这个类是本地线程变量,这些变量与正常线程计数部分不同,可以独立的初始化变量副本.{@code ThreadLocal}
实例是典型的静态私有属性,可以与其他线程进行联系(例如: 用户ID,事务ID,线程安全)
例如,下面的类给线程产生了本地唯一标识符.一个线程的ID首次使用{@code ThreadId.get()}方法指定.且在子调用中保持不变.
每个线程持有这个本地线程变量的显示引用(只要线程存活且本地线程实例可以访问).当线程销毁之后,所有的副本都会被垃圾回收.
```

```java
public class ThreadId {
    // Atomic integer containing the next thread ID to be assigned
    private static final AtomicInteger nextId = new AtomicInteger(0);

   // Thread local variable containing each thread's ID
    private static final ThreadLocal;
    Integer; threadId =
        new ThreadLocal&lt;Integer&gt;() {
            &#64;Override protected Integer initialValue() {
                return nextId.getAndIncrement();
        }
     };

     // Returns the current thread's unique ID, assigning it if necessary
    public static int get() {
        return threadId.get();
    }
}
```

#### ThreadLocal属性

```markdown
1. threadLocalHashCode
本地线程依赖每个线程的线性探测hashmap连接到每个线程.本地线程对象可以作为key,通过threadLocalHashCode.进行搜索.这是一种自定义的hashCode,可以处理hash碰撞.
2. nextHashCode:AtomicInteger
当前hashCode,原子性更新,初始值为0
3. HASH_INCREMENT
生成的hashCode变化量
```

#### ThreadLocal常用方法

##### int nextHashCode()

返回当前本地线程变量的hashcode,并跳转到一个hashcode,原子性操作.

##### T initialValue()

```markdown
返回当前线程的本地线程变量的初始化值,这个方法会在第一次调用get()方法的时候调用.除非之前调用过set()方法,否则这个方法不会被其他线程调用.
正常情况下,每个线程至多调用一次,但是可以由于{@code remove}和{@code get}连续调用再次被调用.
这个实现返回了null,如果需要非null的初始值,必须要子类重写这个方法。可以使用匿名内部类来实现。
```

##### ThreadLocal<S> withInitial(Supplier<? extends S> supplier)

创建本地线程变量,初始化值可以使用get()方法获取

##### T get()

返回当前本地线程变量副本,如果当前线程没有值,可以使用initialValue()进行初始化.

##### boolean isPresent()

确定当前本地线程变量是否有值,有值则返回true

##### void set(T value)

设置当前本地线程变量的值为value,大多数子类不需要对其进行重写

##### void remove()

移除当前本地线程变量,如果之后调用了get方法则会重新进行初始化

##### ThreadLocalMap getMap(Thread t)

获取本地线程变量映射表,可以在`InheritableThreadLocal`中重写

##### void createMap(Thread t, T firstValue)

创建本地线程变量的映射表,可以在`InheritableThreadLocal`中重写

#### SuppliedThreadLocal介绍

这个是ThreadLocal的内部类,是本地线程变量的扩展,用于指定Supplier,从而获取初始值

```java
// 初始值属性
private final Supplier<? extends T> supplier;
// 初始化方法
protected T initialValue()
```

#### ThreadLocalMap

本地线程变量映射表

```markdown
线程本地变量的映射表是一个自定义的hashmap，仅仅用于维护线程本地变量。外部不可以使用线程本地变量的操作。这个类是包私有的。运行在线程类中声明属性。使用线程本地变量的操作。这个类是包私有的。运行在线程类中声明属性。自动的对其进行垃圾回收。
```

##### ThreadLocalMap属性

```markdown
1. INITIAL_CAPACITY
初始化容量,默认16,应当是2的n次方
2. Entry[] table
hash表,长度为2的n次方
3. size
表条目的数量
4. threshold
下次需要扩容的大小
```

##### ThreadLocalMap常用方法

###### setThreshold(int len)

重新设置容量的大小,需要保证最低2/3的加载因子

```java
threshold = len * 2 / 3;
```

###### int nextIndex(int i, int len)

获取i的下一个位置索引

###### int prevIndex(int i, int len)
获取i的上一个位置索引

###### Entry getEntry(ThreadLocal<?> key)

使用key获取条目，这个方法用于获取快速路径，否则会推迟到使用@getEntryAfterMiss方法，这样做主要是提升直接命中的性能。

###### Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e)
处理entry表中key没有找到的情况下,用于填充hash槽

###### void set(ThreadLocal<?> key, Object value) 

设置key/value信息

###### void remove(ThreadLocal<?> key)

移除指定本地线程变量的条目

###### void replaceStaleEntry(ThreadLocal<?> key, Object value,int staleSlot)

```markdown
使用指定key代替旧的entry.传递的值存储在entry中,无论指定key的entry是否存在
由于边缘效应,这个方法会擦除运行体中包含旧entry的所有entry.这个运行体指的是两个空槽直接的序列.
	key: key值
	value key对应的value
	staleSlot 搜索key时首次遇到的槽编号
```

