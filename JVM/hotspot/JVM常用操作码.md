### JVM常用操作码

| 操作码            | 功能                                                         |
| ----------------- | ------------------------------------------------------------ |
| IALOAD            | 从数组中加载int类型                                          |
| DUP               | 复制顶层的操作数栈的值                                       |
| AASTORE           | 存储到引用数组中(stack: ... ,数组引用,索引位置,存储的值)     |
| GOTO              | 跳转到指定位置处(相对地址),用于分支控制                      |
| I2C               | int 转换为char类型(使用栈中一个元素)                         |
| I2B               | int 转换为byte类型(使用栈中一个元素)                         |
| I2D               | int转换为double类型(使用栈中一个元素)                        |
| I2F               | int转换为float类型(使用栈中一个元素)                         |
| I2L               | int转换为long类型(使用栈中一个元素)                          |
| I2S               | int转换为short类型(使用栈中一个元素)                         |
| IADD              | 两个值相加(使用栈中两个元素)                                 |
| IAND              | 按位与(stack:...,值1,值2 --> 按位与的结果)                   |
| ICONST            | 存储一个-1 - 5之间的值,否则会抛出异常                        |
| IDIV              | 整数的除法,使用两个操作数                                    |
| IF_ACMPEQ         | 两个操作数,满足相等比较条件则进入分支(stack...,值1,值2)      |
| IF_ACMPNE         | 两个操作数不相等,则进入分支                                  |
| IF_ICMPEQ         | 如果两个操作数比较相等,则进入分支                            |
| IF_ICMPGE         | 如果操作数1大于等于操作数2,则进入分支条件                    |
| IF_ICMPGT         | 如果操作数1大于操作数2,则进入分支                            |
| IF_ICMPLE         | 如果操作数1小于等于操作数2,进入分支                          |
| IF_ICMPLT         | 如果操作数1小于操作数2,进入分支                              |
| IF_ICMPNE         | 如果两个操作数不相等,进入分支                                |
| IFNONNULL         | 如果操作数非空,进入分支                                      |
| IFNULL            | 如果操作数为空,进入分支                                      |
| IINC              | 本地变量自增常量值                                           |
| ILOAD             | 从本地变量加载int值到栈上                                    |
| IMUL              | 栈上的两个操作数相乘,结果入栈                                |
| INEG              | 栈上操作数取相反数                                           |
| IXOR              | int类型按位异或                                              |
| INSTANCEOF        | 确定对象是否为指定的类型                                     |
| Instruction       | 所有java字节码的抽象超类                                     |
| BasicType         | 基础类型(包括8中基本数据类型和void类型)                      |
| BREAKPOINT        | 断点,jvm独有的属性,默认忽略                                  |
| FieldInstruction  | get/put方法的超类                                            |
| FieldOrMethod     | InvokeInstruction和FieldInstruction的超类                    |
| GETFIELD          | 从对象中获取属性(stack:...,objref --> value或者是stack: ...,objref-> val.word1,val.word2) |
| GETSTATIC         | 获取类的静态属性                                             |
| GotoInstruction   | Goto的超类                                                   |
| InvokeInstruction | INVOKExxx的超类                                              |
| INVOKEINTERFACE   | 调用接口方法(stack: ... ref [arg1,[arg2...]])                |
| INVOKESPECIAL     | 调用实例方法,超类的特殊处理方法,                             |
| INVOKESTATIC      | 调用类的静态方法                                             |
| INVOKEVIRTUAL     | 调用实例的方法,根据类进行分配                                |
| JSR               | 跳转到子程序                                                 |
| LoadClass         | 用于启动类加载以及类引用问题的类                             |
| MONITORENTER      | 对象的入口监视器                                             |
| MONITOREXIT       | 对象的出口监视器                                             |
| NEW               | 创建新对象                                                   |
| NEWARRAY          | 创建基本类型的数组                                           |
| NOP               | 空操作                                                       |
| ObjectType        | 对象类型                                                     |
| POP               | 出栈操作                                                     |
| POP2              | 出栈两个元素                                                 |
| PopInstruction    | 栈操作超类                                                   |
| PUSH              | push操作的包装类,常用实现为BIPUSH,LDC,xCONST_n               |
| PushInstruction   | push的超类,常用实现为iload,ldc,sipush,dup,iconst             |
| PUTFIELD          | 存放属性到对象中                                             |
| PUTSTATIC         | 存放静态值到class中                                          |
| ReferenceType     | 引用类型,对象或者数组类型的超类                              |
| RET               | 子程序的返回值                                               |
| RETURN            | void方法的返回值                                             |
| SALOAD            | 从数组中加载short类型                                        |
| SASTORE           | 存储到short数组中                                            |
| Select            | LOOKUPSWITCH和TABLESWITCH的超类                              |
| SWAP              | 交换栈顶的两个操作数                                         |
| Type              | 所有java类型的超类                                           |

注意到float类型/double类型,底层的实现与int类型不同,所以相应类型操作数进行运算的时候,需要使用指定的运算符进行计算.这里不列出.

详情参考hotspot的描述,路径名称为`com.sun.org.apache.bcel.internal.generic`下的描述.