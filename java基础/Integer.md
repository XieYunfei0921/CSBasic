#### Integer介绍

```markdown
Integer是int类型的包装类,一个对象中包含单个int属性.除此之外,一个类提供多种转换int和string的方法.
实现注意: 实现中对为运算进行了处理,例如@highestOneBit(int)和@numberOfTrailingZeros(int)方法
```

#### Integer属性

```markdown
1. 范围参数
MIN_VALUE = 0x80000000
MAX_VALUE = 0x7fffffff
2. char[] digits
字符集(用于处于多种进制的运算)
val= {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
    };
3. SIZE = 32
位数量
4. BYTES = SIZE / Byte.SIZE
字节数量
5. int value
Integer包含的int值
```

#### 常用方法

##### int compareTo(Integer anotherInteger)

与指定的值进行比较,返回比较结果

##### int compare(int x, int y)

指定的两个值进行比较

#####  int numberOfLeadingZeros(int i)

获取前导零的数量

```java
public static int numberOfLeadingZeros(int i) {
        // HD, Count leading 0's
        if (i <= 0)
            return i == 0 ? 32 : 0;
        int n = 31;
        if (i >= 1 << 16) { n -= 16; i >>>= 16; }
        if (i >= 1 <<  8) { n -=  8; i >>>=  8; }
        if (i >= 1 <<  4) { n -=  4; i >>>=  4; }
        if (i >= 1 <<  2) { n -=  2; i >>>=  2; }
        return n - (i >>> 1);
}
```



##### int numberOfTrailingZeros(int i)

获取后缀零的数量

```java
 public static int numberOfTrailingZeros(int i) {
        // HD, Figure 5-14
        int y;
        if (i == 0) return 32;
        int n = 31;
        y = i <<16; if (y != 0) { n = n -16; i = y; }
        y = i << 8; if (y != 0) { n = n - 8; i = y; }
        y = i << 4; if (y != 0) { n = n - 4; i = y; }
        y = i << 2; if (y != 0) { n = n - 2; i = y; }
        return n - ((i << 1) >>> 31);
 }
```

##### int sum(int a, int b)

求和

##### int max(int a, int b)

取最大值

##### int min(int a, int b)

取最小值

##### int parseInt(String s, int radix)

将指定字符串转换为指定进制的数值

##### Integer valueOf(int i)

返回代表指定值i的包装类对象,这个方法会优先获取-128-127范围内的缓存,如果查找不到则会自动新建

```java
public static Integer valueOf(int i) {
        if (i >= IntegerCache.low && i <= IntegerCache.high)
            return IntegerCache.cache[i + (-IntegerCache.low)];
        return new Integer(i);
    }
```



#### Integer缓存的设计(常量池）

##### 介绍

```markdown
IntegerCache:
这个缓存支持对象自动装箱的辨识跟你,值在-128-127之间.
这个缓存首次使用的时候被初始化,缓存大小可以使用JVM参数{@code -XX:AutoBoxCacheMax=<size>}控制.在虚拟机初始化时,会设置IntegerCache.high参数并保存在私有的系统参数中(jdk.internal.misc.VM)
```

##### 属性

```markdown
low下限值
high 上限值
cache 缓存表
```

