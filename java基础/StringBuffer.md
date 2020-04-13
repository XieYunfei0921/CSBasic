#### StringBuffer介绍

```markdown
线程安全，可变的字符序列。类似于String,但是可以修改.长度和序列内容可以通过响应的方法改变.StringBuffer是线程安全的,这个方法在必要的部分是同步的,以便于特定实例的操作是按照串行执行的.主要的操作主要是@append和@insert方法,这个是可以接受任何类型数据的.append方法会将新的字符添加到当前序列的末尾,insert支持指定位置的插入.无论是何种操作,这个类都会同步字符串操作,而不是同步原始的序列.注意到StringBuffer是线程安全的,如果append或者insert传入了共享的序列.调用代码时必须保证操作包含对源序列的连续且不变的视图.可以通过调用者在操作期间持有锁来解决.
每个string buffer都有一个容量,只要没有超出这个容量,就不需要分配新的内部缓冲数组.如果内部缓冲区溢出则会自动扩容.jdk 5开始,单线程使用的时候可以使用StringBuilder,这个类不需要同步
```

#### StringBuffer属性

```markdown
1. String toStringCache
上次使用toString方法的值缓存,修改的时候会被清除
```

#### 常见方法

##### int newCapacity(int minCapacity)

StringBuffer的扩容策略

```java
private int newCapacity(int minCapacity) {
        // overflow-conscious code
        int newCapacity = (value.length << 1) + 2;
        if (newCapacity - minCapacity < 0) { // 翻倍后仍然不足的情况
            newCapacity = minCapacity;
        }
        return (newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0)
            ? hugeCapacity(minCapacity) // 最大容量 MAX_INT-8
            : newCapacity;
}
```

##### synchronized StringBuffer append(CharSequence s)

```markdown
添加指定的字符序列到这个序列中
字符序列按照顺序添加到当前缓冲区中,并更新当前序列的长度
方法是同步的,但是源序列不是同步的
```

##### synchronized StringBuffer delete(int start, int end)

```markdown
移除缓冲区指定区域的字符
```

##### synchronized StringBuffer replace(int start, int end, String str)

```markdown
替换字符序列指定范围的内容
```

##### synchronized String substring(int start)

```markdown
获取指定范围的串
```

##### synchronized void writeObject(java.io.ObjectOutputStream s)

序列化

```java
private synchronized void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        java.io.ObjectOutputStream.PutField fields = s.putFields();
        char[] val = new char[capacity()];
        if (isLatin1()) {
            StringLatin1.getChars(value, 0, count, val, 0);
        } else {
            StringUTF16.getChars(value, 0, count, val, 0);
        }
        fields.put("value", val);
        fields.put("count", count);
        fields.put("shared", false);
        s.writeFields();
}
```

##### private void readObject(java.io.ObjectInputStream s) 

反序列化

```java
private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        java.io.ObjectInputStream.GetField fields = s.readFields();
        char[] val = (char[])fields.get("value", null);
        initBytes(val, 0, val.length);
        count = fields.get("count", 0);
}
```

#### StringBuilder

```mariadb
字符可变序列,这个类提供了StringBuffer的API.但是不能保证同步.这个类用于单线程使用的情况.可能的话,这个类运行的速度快于StringBuffer.
StringBuilder的主要操作为@append和@insert方法.
StringBuilder有容量的限制,只要字符序列的长度没有超出容量,就不需要额外的分配缓冲区.如果超过容量,则会自动的扩容.
StringBuilder是不安全的,如果需要使用线程安全的话,需要使用StringBuffer类
```

