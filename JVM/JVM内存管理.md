### **自动内存管理**

---

1. [运行时数据区域](# 运行时数据区域)
2. [HotSpot虚拟机探秘](# HotSpot虚拟机探秘)
3. [OOM异常](# OOM异常)

---

####  运行时数据区域

1. [程序计数器](# 程序计数器)

2. [java虚拟栈](# 虚拟栈)

3. [本地方法栈](# 本地方法栈)

4. [java堆](# java堆)

5. [方法区](# 方法区)

6. [运行时常量池](# 运行时常量池)

7. [直接内存](# 直接内存)

   ---

   ###### 程序计数器

   > 定义: 程序计数器是一块较小的内存空间,可以看做当前线程所执行的字节码的**行号指示器**.
   >
   > 字节码解释器的功能是通过改变这个**行号指示器**从而确定下一个需要执行的字节码指令,也是程序控制流的指示器,分支,循环,跳转,异常处理线程恢复等等基础功能都要通过这个**行号指示器**完成.

   java虚拟机中多线程是通过线程轮流切换,分区处理器的执行时间的方式来进行的.任意确定的时刻,一个处理器都只会执行一条线程的指令.

   因此,为了使得线程恢复到正确的位置,每个线程都需要设置一个程序计数器,用于存储每个线程的**行号计数器**.因此线程之间的程序计时器之间是没有关系的.

   >如果线程中执行的是一个java方法,这个计数器记录的是正在执行的虚拟机字节码指令的地址,如果这个执行的是本地方法,这个计数器的值为空(Undefined).这个内存区域时没有规定OOM的区域.

   ##### 虚拟栈

   > 与程序计数器类似,java虚拟机栈也是线程私有的.生命周期同线程.虚拟机栈描述了java执行的线程内存模型.
   >
   > 当每个方法被执行的时候,java虚拟机都会创建一个**栈帧**.主要的目的是存储**局部变量表**,**操作数栈**,**动态链接**,**方法出口**等信息.
   >
   > 每个方法被调用直至执行完成的过程,就对应栈帧在虚拟机栈中从入栈到出栈的过程.
   >
   > 通常将java内存区域粗略的使用**堆内存**和**栈内存**划分,直接继承了C/C++ 的内存划分方法.通常java中的栈指的是虚拟机栈.

   **局部变量表**存放了编译期间各种java虚拟机的基本数据类型(boolean,byte,char,short, int,float,long,double),对象引用(reference类型， 它并不等同于对象本身， 可能是一个指向对象起始地址的引用指针， 也可能是指向一个代表对象的句柄或者其他与此对象相关的位置 )和**返回地址**(指向一条字节码指令的地址).

   这些数据类型在**局部变量表**中的存储空间以**局部变量槽**(slot的形式表示).其中64位长度的long类型和double类型会占用两个变量槽,其余类型只会占用一个变量槽.**局部变量表**的内存空间在编译期间完成分配.方法运行期间不会改变局部变量表的大小.这里的"大小"指的是变量槽的数量,具体关于虚拟机真正到底使用了多少的内存空间(1槽=32/64B)需要有具体的虚拟机决定.

   >在java虚拟机规范中,内存区域定义了两种形式的异常:
   >
   >如果线程请求的栈深度大于虚拟机允许的最大深度,将抛出@StackOverflowError 异常.如果java虚拟机站容量可以动态扩展,当无法再进行扩展的时候,则会抛出@OutOfMemoryError 异常.

   ##### 本地方法栈

   > **本地方法栈**与虚拟机栈的作用是相似的,指示虚拟机栈为虚拟机执行java方法服务,而本地栈为本地(Native)方法服务.

   ##### java堆

   > java堆是虚拟机管理的内存中最大的一块.java对是被所有线程所共享的一块内存区域.在虚拟机启动的时候创建.这个内存区域的唯一目的就是存放对象实例,java中的几乎所有对象实例都存放在这里.
   >
   > java虚拟机规范中指出,所有对象实例以及数组都应当在堆上分配.
   >
   > 这里使用了"几乎"这个词,表示随着java的发展,现在已经能够开到之后可能会出现**值支持**类型的支持,由于编译技术进步,尤其是**逃逸分析**技术的进步,**栈上分配**,**标量替换**等优化手段已经导致了变化.所以说堆上分配实例已经不是绝对.

   java堆是垃圾收集器管理的内存区域.一些资料中称为GC堆,从回收内存的角度来看,由于现代大多数垃圾收集器基于**分代收集理论**设计的.所以java堆中经常出现的`新生代`,`老年代`,`永久代`,`Eden空间`,`From Survivor空间`,`To Survivor空间`.作为主流的HotSpot虚拟机,内部的垃圾收集器全部基于**经典分代**设计.

   如果从分配内存的角度来看,所有线程共享的java堆中可以划分为多个线程西游的分配缓冲区(TLAB: Thread Local Allocation Buffer ).设置这个用于提示分配时的效率,无论通过什么角度,都不会改变java堆中存储内容的共享,无论什么区域存储的都是对象实例.将java堆细分的目的只是为了更好的分配和释放内存.

   根据java虚拟机规范中指定,java堆可以处于无论上不连续的内存空间中.但是在逻辑上视作连续的.对于大的对象(数组对象),多数虚拟机处于实现简单,存储高效的考虑,会要求连续的内存空间.

   java的堆大小可以为固定的,也可以设置为可以扩展的(-Xmx和-Xms设定).如果java堆没有完成实例内存的分配,且堆也没有办法进行扩展的时候,会抛出@OutOfMemoryError 异常.

   ##### 方法区

   > 与java对类似,是各个线程共享的部分,由于存储已经被虚拟机加载的类型信息,常量,静态变量,即时编译器编译后的代码缓存等数据.虽然java虚拟机将其描述为一个逻辑部分,别名非堆(Non-Heap).目的是与java堆分开.

   java虚拟机规范中对方法区的约束比较宽松,除了和java堆一样不需要连续的内存和可以选择固定大小或者可扩展的情况之外.甚至可以选择不实现垃圾收集.所以,垃圾收集在这个区域来说时比较少出现的.(但是并不是意味着进入了永久代了).

   ##### 运行时常量池

   **运行时常量池**(Runtime Constant Pool)是方法区的一部分.class文件除了有**类的版本**,**字段**,**方法**,**接口**等描述信息之外,还有一项信息是**常量池表**.用于存放编译器的各种字面量和符号引用.这部分是在类加载之后放到方法区的运行时常量池中.

   java虚拟机对于class文件的每一部分(包括常量池),有这严格的格式规定.如每一个字节用于存储哪种数据都必须符合规范才会被虚拟机认可,并加载执行.但是对于运行时常量池,并没有任何细节的要求,不同供应商的虚拟机提供的方式不同.一般的,除了保存class文件外,还会把符号引用翻译出来的直接引用存储在运行时常量池中.

   > 运行时常量池相对于class文件常量池的另一个特征为具备动态性.java语言不要求常量池必须要编译器才能产生.也就是,并非置入calss文件中的常量池才能进入方法区运行时常量池.运行期间也可以将新的常量放入池中.这种应用最广泛的是String.intern()
   >
   > 既然运行时常量池是方法区的一部分,自然会受到方法区内存的限制.当没有内存分配的时候便会OOM.

   ##### 直接内存

   >  **直接内存**并不是虚拟机运行时数据区的一部分,也不是java虚拟机规范中定义的内存区域.这部分内存也会被频繁使用,且可能导致OOM的发生.
   >
   > JDK1.4之后,引入了NIO,引入了基于通道和缓冲区的IO方式,可以使用本地函数库直接分配堆外内存.通过一个java堆中的@DirectByteBuffer对象作为内存引用进行操作.这样可以提供性能,避免java堆和native堆的数据拷贝.
   >
   > 本机的直接内存分配不会受到java堆大小的限制,但是会受到本机总内存的影响.

   ---

   **基本概念总结**

   java虚拟机在执行java程序的过程中会把它管理的内存划分为多个不同区域.用途不同,创建和销毁的时间不同.总体来说可以使用下图来描述:

   <img src="E:\截图文件\JVM结构.png" style="zoom:67%;" />

#### HotSpot虚拟机对象探秘

使用最常用的虚拟机HotSpot和最常用的内存区域java堆为例。探讨HotSpot虚拟机在java堆中对象的分配，布局和访问的过程。

1.  对象的创建

   java是面向对象的，java程序运行过程中无时无刻都有对象被创建出来，创建对象(除了复制和反序列化之外)仅仅使用了一个**new**关键字.在虚拟机中,对象创建又是如何.

   java遇到一条字节码new指令的时候:

   1.  首先检查这个指令的参数是否能够定位到常量池中定位到一个类的引用符号.
   2.  检查这个符号对应的类是否被加载,解析或者初始化过.
   3.  如果没有加载,需要进行类的加载

   通过了类加载之后,需要为新生的对象分配内存,对象所需要的内存再类加载完成之后就客户以确定,为对象分配空间相当于把一块内存区域从java堆中划分出来.

   > 假设java堆内存时规整的,存在一个指针将分配完的区域和未分配的区域分开.分配指定数量内存量m,就相当于将指针@p+=m个位置.这个分配方式叫做**指针碰撞**.
   >
   > 如果java的堆不是规整的,使用的内存和未使用的内存交错在一起,就没有办法进行直接的指针碰撞了,虚拟机必须维护一个列表(操作系统中,称这个表为空闲块列表),用于记录那些内存块时可用的.在分配的时候划分一块足够的内存给对象实例(根据不同系统的需要可以有不同的划分方法),常见的划分算法有,**最佳拟合法**,**首次拟合法**,**最差拟合法**等等.不同的划分算法会导致不同大小的内存碎片存在.
   >
   > java对是否规整由所采用的的垃圾收集器是否带有**空间压缩整理**的能力,也就是内存紧缩的能力,如果需要拥有这个能力,释放内存的时候需要更多的时间进行**内存紧缩**.
   >
   > 因此你在Serial,ParNew等带有园所的整理过程的收集器,系统采用的分配算法为**指针碰撞**法.当使用CMS这种基于清除算法的收集器的时候,理论上采用空闲块列表来分配内存合适.

此外,还需要考虑的问题就是,对象创建时虚拟机进行频繁的行为,即使仅仅修改一个指针的指向,在并发情况下也是不安全的.可能操作,对于对象A已经分配了内存,但是对象B又使用了相同的指针,虽然可以通过使用读写互斥量控制指针的指向,但是涉及到内存紧缩的情况下,运行又是相当慢的.为了解决这个问题.

+ 对内存分配的操作进行同步处理

  实际上虚拟机采样CAS配上失败重试保证更新操作的原子性(容错+原子性).

+ 把内存分配的动作按照线程划分在不同的内存空间中进行

  即一个线程分的一部分内存区域,称作**本地线程分配缓冲**(TLAB).哪个线程需要分配内存,就在哪个本地缓冲区中分配,只有当本地缓冲区内存使用完毕的时候,才需要分配新的内存区域,指示才需要使用同步锁定的方法.

  这个方法可以通过-XX: +/- UseTLAB参数设定,但是会造成较多的内存碎片.

内存分配完毕,虚拟机需要将分配到的内存区间(不包括对象头),**初始化为零值**.如果使用TLAB的话,这个工作会在TLAB分配之前完成.这个操作保证了java代码中实例的字段可以不用初始值进行使用,使程序访问的数据类型为对应的零值.

接下来,java虚拟机需要对对象进行必要的设置,例如**对象是哪个类的实例**,如何可以找到**类的元数据信息**,**对象的hash码**(在真正调用obj.hashCode()时才会真正的进行计算).

根据虚拟机运行状态的不同,是否使用**偏向锁**.

上面工作完成之后,一个对象就产生了,但是在java程序的角度来看,对象创建才正式开始(**构造函数**),即class文件中的<init>()方法还没有执行.所有字段都是默认的零值.对象需要其他的资源和状态信息还没有按照预定的意图设置完成.

一般来说,有字节码流中new指令后面是否跟随invokespecial指令决定.java编译器会遇到new关键字的地方同时生成两条字节码指令,如果使用其他方式生成则不一定.

下面是hotspot中关于虚拟机字节码接收器的代码片段.可以解释hotspot的运行过程.

```c++
// 确保常量池中存放的是已解释的类(为处理才可以进行处理)
if (!constants->tag_at(index).is_unresolved_klass()) {
    
    // 断言确保是klassOop和instanceKlassOop（这部分下一节介绍）
    oop entry = (klassOop) *constants->obj_at_addr(index);
    assert(entry->is_klass(), "Should be resolved klass");
    klassOop k_entry = (klassOop) entry;
    assert(k_entry->klass_part()->oop_is_instance(), "Should be instanceKlass");
    instanceKlass* ik = (instanceKlass*) k_entry->klass_part();
    
    // 确保对象所属类型已经经过初始化阶段，初始化才可以进行操作
    if ( ik->is_initialized() && ik->can_be_fastpath_allocated() ) {
        // 取对象长度
        size_t obj_size = ik->size_helper();
        oop result = NULL;
        // 记录是否需要将对象所有字段置零值
        bool need_zero = !ZeroTLAB;
        // 是否在TLAB中分配对象
        if (UseTLAB) {
        result = (oop) THREAD->tlab().allocate(obj_size);
        }
        if (result == NULL) {
        need_zero = true;
        // 直接在eden中分配对象
        retry:
        HeapWord* compare_to = *Universe::heap()->top_addr();
        HeapWord* new_top = compare_to + obj_size;
        // cmpxchg是x86中的CAS指令， 这里是一个C++方法，
        //    通过CAS方式分配空间， 并发失败的
        话， 转到retry中重试直至成功分配为止
        if (new_top <= *Universe::heap()->end_addr()) {
            if (Atomic::cmpxchg_ptr(
                new_top, Universe::heap()->top_addr(), 
                compare_to) != compare_to){
                goto retry;
            }
            result = (oop) compare_to;
        }
    }
    if (result != NULL) {
    // 如果需要， 为对象初始化零值
    if (need_zero ) {
        HeapWord* to_zero = (HeapWord*) result + sizeof(oopDesc) / oopSize;
        obj_size -= sizeof(oopDesc) / oopSize;
        if (obj_size > 0 ) {
        memset(to_zero, 0, obj_size * HeapWordSize);
        }
    }
    // 根据是否启用偏向锁， 设置对象头信息
    if (UseBiasedLocking) {
    	result->set_mark(ik->prototype_header());
    } else {
    	result->set_mark(markOopDesc::prototype());
    }
    result->set_klass_gap(0);
    result->set_klass(k_entry);
    // 将对象引用入栈， 继续执行下一条指令
    SET_STACK_OBJECT(result, 0)
    UPDATE_PC_AND_TOS_AND_CONTINUE(3, 1);
    }
  }	
}
```

2.  对象的内存布局

   对象在虚拟机内包含三个部分,**对象头**,**实体数据**,**对齐补充**.

   + **对象头**

     HotSpot虚拟机对象的对象头包含两类信息,第一类使用于存储对象自身运行时数据,例如:

     + hash码
     + GC分代年龄
     + 锁状态标志
     + 线程持有的锁
     + 偏向线程ID
     + 偏向时间戳

     这个部分在32位和64位虚拟机中分别为32和64个bit.官方称作*Mark Word*.对象需要存储的运行数据很多,超多了32/64位Bitmap结构所能记录的最大限度.但是对象头中的信息与对象本身定义无关的额外存储成本.考虑到虚拟机的空间效率,*Mark Word*被设计成一个有着动态定义的数据结构，以便在极小的空间存储尽可能多的数据，根据对象的状态赋予自己的存储空间。例如在32位的HotSpot虚拟机中，对象未被同步锁锁定的状态下，*Mark Word*中32个比特位其中25个比特位用于存储对象哈希码，4个比特位用于存放分代年龄。2个比特位用于存储锁标志位，1个比特位为0，其他状态(轻量级锁定,重量级锁定,GC标记,可偏向)存储内容如下所示:

     | 存储内容                           | 标志位 | 状态       |
     | ---------------------------------- | ------ | ---------- |
     | 对象hash码,对象分代年龄            | 01     | 未锁定     |
     | 指向锁记录的指针                   | 00     | 轻量级锁定 |
     | 指向重量级锁的指针                 | 10     | 重量级锁定 |
     | 空                                 | 11     | GC标记     |
     | 偏向线程ID,偏向时间戳,对象分代年龄 | 01     | 可偏向     |

     对象头另一部分内容为类型直至,即对象指向它的类型元数据的指针,java虚拟机通过这个指针确定对象是哪个类的实例.并不是所有虚拟机的实现都必须在对象数据上保留类型指针,换句话说,查找对象的元数据信息不一定要经过对象本身.此外,如果对象是一个java数组,name在对象头中有一块用于记录数组长度的数据,因为虚拟机需要通过java对象的元数据信息确定java对象的大小.但是如果数组长度无法确定.则无法通过元数据确定数组的大小.

     下面的代码`markOop.cpp`中注释了32位虚拟机Mark Word的存储布局

     ```c++
     // Bit-format of an object header (most significant first, big endian layout below):
     //
     // 32 bits:
     // --------
     // hash:25 ------------>| age:4 biased_lock:1 lock:2 (normal object)
     // JavaThread*:23 epoch:2 age:4 biased_lock:1 lock:2 (biased object)
     // size:32 ------------------------------------------>| (CMS free block)
     // PromotedObject*:29 ---------->| promo_bits:3 ----->| (CMS promoted object)
     ```

     无论是从父类继承韩式子类中的字段,都必须记录起来,这部分存储顺序会影响虚拟机分配策略(-XX： FieldsAllocationStyle参数).和字段在java源码中定义顺序的影响.HotSpot中分配顺序为long/double,int,short/char,byte/boolean,oop.从默认的分配策略可以看出,相同宽度的总是分配带一起存放,在满足这个条件下,父类中的变量会出现在子类之前.

     如果HotSpot中+XX: CompactFields参数值为true,那么子类中较窄的变量允许插入父类变量的空隙中,节省空间.

   + 对其填充

     这个并不是一定要存在的,仅仅起到占位符的作用.但是hotspot虚拟机的自动内存管理器系统要求对象起始地址必须是8的整数倍数.由于对象头已经是8的整数倍数,因此只需要使用对其填充将实例数据部分对齐.

3. 对象访问的定位

   创建对象的目的是为了之后使用这个对象,java程序会通过栈上引用数据来操作堆上的具体对象.**引用**就是一个执行对象的引用,并没有定义通过什么方式去定位,访问的堆中对象的具体位置,所以访问对象也是由虚拟机实现的,主流的访问方式有**句柄**和**直接指针**两种.

   如果使用句柄访问,则需要划分出句柄池,引用中存储的就是对象的句柄地址,句柄中包含了对象的**实例数据**和类型数据各自的具体地址信息.

   如果使用直接指针访问,java对中对象的内存布局必须考虑如何防止类型数据相关的信息,**引用中存储的就是对象地址**.如果需要访问对象本身的话,就可以实现直接访问.

   两种方式各有优劣，句柄访问的最大好处就是引用中存储的是稳定句柄地址，在对象被移动时只会改变句柄中**实例数据**的指针。引用本身并不会发生变化。

   + 句柄访问(间接访问)

     <img src="E:\截图文件\句柄访问.png" style="zoom:67%;" />

   + 直接指针访问(直接访问)

     <img src="E:\截图文件\直接地址访问.png" style="zoom:67%;" />

   直接指针方法的方式速度更快,hotSpot中常见使用直接访问,但是从软件开发的角度来看,句柄访问也很常见.

#### OOM异常

使用IDE进行调试运行的时候，记得配置JVM参数。

`-verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintDetails -XX:SurvivorRatio=8`

##### java堆溢出

java堆用于存储对象实例,只要不断的创建对象,保证GC Root到对象之间存在有可到达路径,从而避免被垃圾回收,随着对象数量增加,超出堆最大容量之后,就会产生内存溢出.

下述的代码中设置java堆的大小为20MB,不可以扩展(设置最小值-Xms和最大值-Xmx一致是为了避免自动扩展).通过参数`-XX: +HeapDumpOnOutOfMemoryError`可以让虚拟机出现内存溢出的时候Dump出当前内存堆转储快照,以便进行事后分析.

```java
/**
* VM Args： -Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError
* @author zzm
*/
public class HeapOOM {
    static class OOMObject {
    }
    
    public static void main(String[] args) {
        List<OOMObject> list = new ArrayList<OOMObject>();
        while (true) {
        	list.add(new OOMObject());
        }
    }
}
```

上述程序会导致内存溢出

```shell
java.lang.OutOfMemoryError: Java heap space
Dumping heap to java_pid3404.hprof ...
Heap dump file created [22045981 bytes in 0.663 secs]
```

java堆内存异常时实际应用中常见的内存溢出异常.出现异常信息的同时会进一步的显示`java heap space`.

处理这类内存溢出的方法时,通过**内存映像分析工具**(Ecilpse Memory Analyzer)对Dump出来的堆转储进行快照分析.

第一步,确定导致OOM的对象是否必要,即需要确定到底是**内存泄漏**(分配的内存没有回收)还是**内存溢出**(运行内存量不足).

如果是内存泄漏,可以通过工具查看泄漏对象到GC Roots的引用链,找到对象通过何种路径找到GC Root,根据泄漏对象的类型信息以及GC Root引用链找到什么地方产生了内存泄漏.

如果不是内存泄漏,就需要检查java虚拟机的堆参数设置,与机器内存的对比,看看是否可以向上调整.从代码上检查是否有一些代码生命周期过程,持有状态时间过程,存储结构设计不合理情况.减少程序运行时内存消耗的情况.

##### 虚拟机栈和本地方法栈溢出

HotSpot中不区分虚拟机栈和本地方法栈,因此设置`-Xoss`(本地方法栈)的参数虽然可以设置但是并没有什么效果,只能设置`-Xss`确定栈同类.关于栈异常,java虚拟机规范中提供两种类型:

1. 线程请求的栈深度待遇虚拟机允许的最大深度,抛出StackOverflowError异常
2. 如果虚拟机的栈内存允许动态扩展， 当扩展栈容量无法申请到足够的内存时， 将抛出
   OutOfMemoryError异常 .

java虚拟机规范中允许java虚拟机选择是否支持栈的动态扩展,而HotSpot虚拟机选择了不支持扩展,除非申请内存的时候无法获得足够的内存,从而出现OOM,才会扩展栈内存.

可以进行如下实验,尝试下述两种内容是否可以产生OOM异常.

- 使用`-Xss`参数减少栈内存容量

  结果: 抛出StackOverflowError异常,异常出现的时候输出栈深度

- 定义大量的本地变量,增大方法帧中本地变量表的长度

  结果: 抛出StackOverflowError异常,异常出现的时候输出栈深度

+ 减少栈内存的测试代码如下

```java
/**
* VM Args： -Xss128k
*/
public class JavaVMStackSOF {
	private int stackLength = 1;
    public void stackLeak() { // 每次执行栈深度+1
        stackLength++;// 用于表示栈深度度量参数
        stackLeak();
    } 
    public static void main(String[] args) throws Throwable {
		JavaVMStackSOF oom = new JavaVMStackSOF();
        try {
        	oom.stackLeak();
        } catch (Throwable e) {
            // SOF时显示栈深度
            System.out.println("stack length:" + oom.stackLength);
            throw e;
        }
	}
}
```

运行结果形式如下:

```shell
stack length:2402
Exception in thread "main" java.lang.StackOverflowError
    at org.fenixsoft.oom. JavaVMStackSOF.leak(JavaVMStackSOF.java:20)
    at org.fenixsoft.oom. JavaVMStackSOF.leak(JavaVMStackSOF.java:21)
    at org.fenixsoft.oom. JavaVMStackSOF.leak(JavaVMStackSOF.java:21)
```

对于不同版本的虚拟机和操作系统,栈容量最小值可能会有所限制,主要取决于操作系统内存分页的大小.

譬如上述方法中的参数-Xss128k可以正常用于32位Windows系统下的JDK 6， 但是如果用于64位Windows系统下的JDK 11， 则会提示栈容量最小不能低于180K， 而在Linux下这个值则可能是228K .如果低于这个值则可能抛出异常信息.

- 定义大量本地变量的测试代码

  ```java
  public class JavaVMStackSOF {
  private static int stackLength = 0;
  public static void test() {
      long unused1, unused2, unused3, unused4, unused5,
      unused6, unused7, unused8, unused9, unused10,
      unused11, unused12, unused13, unused14, unused15,
      unused16, unused17, unused18, unused19, unused20,
      unused21, unused22, unused23, unused24, unused25,
      unused26, unused27, unused28, unused29, unused30,
      unused31, unused32, unused33, unused34, unused35,
      unused36, unused37, unused38, unused39, unused40,
      unused41, unused42, unused43, unused44, unused45,
      unused46, unused47, unused48, unused49, unused50,
      unused51, unused52, unused53, unused54, unused55,
      unused56, unused57, unused58, unused59, unused60,
      unused61, unused62, unused63, unused64, unused65,
      unused66, unused67, unused68, unused69, unused70,
      unused71, unused72, unused73, unused74, unused75,
      unused76, unused77, unused78, unused79, unused80,
      unused81, unused82, unused83, unused84, unused85,
      unused86, unused87, unused88, unused89, unused90,
      unused91, unused92, unused93, unused94, unused95,
      unused96, unused97, unused98, unused99, unused100;
      stackLength ++;
      test();
      unused1 = unused2 = unused3 = unused4 = unused5 =
      unused6 = unused7 = unused8 = unused9 = unused10 =
      unused11 = unused12 = unused13 = unused14 = unused15 =
      unused16 = unused17 = unused18 = unused19 = unused20 =
      unused21 = unused22 = unused23 = unused24 = unused25=
      unused26 = unused27 = unused28 = unused29 = unused30 =
      unused31 = unused32 = unused33 = unused34 = unused35 =
      unused36 = unused37 = unused38 = unused39 = unused40 =
      unused41 = unused42 = unused43 = unused44 = unused45 =
      unused46 = unused47 = unused48 = unused49 = unused50 =
      unused51 = unused52 = unused53 = unused54 = unused55 =
      unused56 = unused57 = unused58 = unused59 = unused60 =
      unused61 = unused62 = unused63 = unused64 = unused65 =
      unused66 = unused67 = unused68 = unused69 = unused70 =
      unused71 = unused72 = unused73 = unused74 = unused75 =
      unused76 = unused77 = unused78 = unused79 = unused80 =
      unused81 = unused82 = unused83 = unused84 = unused85 =
      unused86 = unused87 = unused88 = unused89 = unused90 =
      unused91 = unused92 = unused93 = unused94 = unused95 =
  	unused96 = unused97 = unused98 = unused99 = unused100 = 0;
  }
      
      public static void main(String[] args) {
          try {
          	test();
          }catch (Error e){
              System.out.println("stack length:" + stackLength);
              throw e;
          }
      }
  }
  ```

  结果表明，物理是栈内存太小或者是定义的变量过于多的时候，抛出的都是SOF。

##### 方法区和运行时常量池的溢出

> 示例:
>
> String::intern是一个本地的方法,作用是如果字符串常量池中包含一个等于这个String对象的字符串,则范湖代表池中这个字符串的引用;否则会将次String对象的字符串添加到常量池中,并且返回这个字符串得String的应用.
>
> ```java
> /**
> * VM Args： -XX:PermSize=6M -XX:MaxPermSize=6M
> */
> public class RuntimeConstantPoolOOM {
>     public static void main(String[] args) {
>         // 使用Set保持着常量池引用， 避免Full GC回收常量池行为
>         Set<String> set = new HashSet<String>();
>         // 在short范围内足以让6MB的PermSize产生OOM了
>         short i = 0;
>         while (true) {
>         	set.add(String.valueOf(i++).intern());
>         }
>     }
> }
> ```
>
> 运行结果就会显示OOM,并指示内存溢出空间为`PermGen space`.在JDK 7之后这里显示的信息会不同.

##### 本机直接内存溢出

直接内存容量大小,可以通过`-XX: MaxDirectMemorySize`参数指定,如果不指定,默认与java堆的最大值`-Xmx`一致,下面的示例中越过了@DirectByteBuffer类直接通过反射获取Unsafe实例,并进行内存分配.虽然@DirectByteBuffer也会抛出内存溢出的异常,但是抛出异常的时候没有真正的想操作系统申请分配内存,而是用过计算的是内存无法分配,就会在代码中手动抛出异常,真实是使用Unsafe::allocateMemory()进行内存的分配.

```java
/**
* VM Args： -Xmx20M -XX:MaxDirectMemorySize=10M
*/
public class DirectMemoryOOM {
    private static final int _1MB = 1024 * 1024;
        public static void main(String[] args) throws Exception {
            Field unsafeField = Unsafe.class.getDeclaredFields()[0];
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            while (true) {
                unsafe.allocateMemory(_1MB);
            }
    }
}
```

可以得到运行结果抛出内存溢出异常，如果程序中有使用直接内存(典型的就是NIO).那就可以考虑检查直接内存方面的内容.