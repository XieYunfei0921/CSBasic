### AtomicInteger

#### 介绍

```markdown
int类型原子更新。可以查看@VarHandle 关于原子属性参数的获取。这个类用作原子性增加的计数器，且不能使用@Integer代替。但是这个类不会继承@Number这样就不能作为数字的类使用。
```

#### 属性

```markdown
jdk.internal.misc.Unsafe U = jdk.internal.misc.Unsafe.getUnsafe()
long VALUE = U.objectFieldOffset(AtomicInteger.class, "value")

+ 值属性,volatile修改之后所有线程可见
volatile int value
```

#### 常用方法

##### int get()

返回当前的value值,受到@VarHandle#getVolatile的影响
##### void set(int newValue)
设置当前值为指定,受到@VarHandle#setVolatile影响
##### void lazySet(int newValue)
设置当前值为@newValue,受到内存影响@VarHandle#setRelease
##### int getAndSet(int newValue)
原子性的设置新值,且设置完毕之后返回旧值,受到内存影响
##### boolean compareAndSet(int expectedValue, int newValue)
原子性的设置新值,受到内存影响(CAS操作),返回true表示更新成功.返回false表示,实际值不等于期待值
##### boolean weakCompareAndSet(int expectedValue, int newValue)
可能会原子性的设置新值.受到内存影响.方法弃用
##### boolean weakCompareAndSetPlain(int expectedValue, int newValue)
原子性的设置新值,受到内存影响
##### int getAndIncrement()
int getAndIncrement()
##### int getAndDecrement()
原子性的减小当前值,受到内存影响
##### int getAndAdd(int delta)
原子性的增加指定值,受到内存影响,返回更新前的值
##### int incrementAndGet()
原子性的增加当前值,受到内存影响
##### int decrementAndGet()
原子性的减少当前值,受到内存影响
##### int addAndGet(int delta)
原子性地增减指定值,受到内存影响
##### int getAndUpdate(IntUnaryOperator updateFunction) 
原子性地更新当前值,返回更新之前的值.函数需要边界无关性,一维更新失败的时候会重新调用.
```java
public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) // 出现更新情况,对当前参数调用边界无关函数
                next = updateFunction.applyAsInt(prev);
            if (weakCompareAndSetVolatile(prev, next)) // 原子性的设置新值并返回旧值
                return prev;
            // 确定是否存在更新,更新则会返回false
            haveNext = (prev == (prev = get()));
        }
    }
```
##### int updateAndGet(IntUnaryOperator updateFunction)
原子性更新,使用指定的边界无关函数,返回更新后的值.这个函数具有边界无关性,因为会重新调用
##### int getAndAccumulate(int x,IntBinaryOperator accumulatorFunction)
原子性的更新当前值,使用给定的函数.并返回之前的值.这个函数需要具有边界无关性,因为可以由于失败而重新调用.这个函数第一个参数是当前值,第二个参数是更新后的值.
##### int accumulateAndGet(int x,IntBinaryOperator accumulatorFunction)
原子性地更新值,使用给定的更新函数,返回更新后的值,这个函数需要具有边界无关性,这个函数第一个参数需要是当前值,第二个参数需要是更新后的值
##### int compareAndExchange(int expectedValue, int newValue)
原子性地设置新值,受到内存影响
##### int compareAndExchangeAcquire(int expectedValue, int newValue)
原子性地设置新值,受到内存影响
##### int compareAndExchangeRelease(int expectedValue, int newValue) 
原子性地设置新值,受到内存影响
##### boolean weakCompareAndSetVolatile(int expectedValue, int newValue)
原子性地设置新值,受到内存影响,设置成功返回true
##### boolean weakCompareAndSetAcquire(int expectedValue, int newValue) 
原子性地CAS操作
### AtomicIntegerArray

#### 介绍

```markdown
int类型的数组,参数可以原子性地更新.
```

#### 属性

```markdown
final VarHandle AA
final int[] array
```

#### 常见方法

##### int length()
返回数组长度
##### int get(int i)
返回数组指定索引位置的元素,受到内容的影响
##### void set(int i, int newValue)
设置索引i位置的新值
##### void lazySet(int i, int newValue)
设置指定i位置的新值,受到内存影响
##### int getAndSet(int i, int newValue)
原子性地设置位置i处的元素值,并返回旧值
##### boolean compareAndSet(int i, int expectedValue, int newValue)
对指定位置i出的元素进行CAS操作
##### boolean weakCompareAndSet(int i, int expectedValue, int newValue)
可能性的对位置i元素进行CAS操作
##### int getAndIncrement(int i)
原子性地增加位置i的元素,并返回旧值
##### int getAndDecrement(int i)
原子性的减小i的大小,并返回旧值
##### int getAndAdd(int i, int delta)
原子性地增加指定值，并返回旧值


