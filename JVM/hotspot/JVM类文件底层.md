#### 权限标识符

jvm中用于控制类的访问权限的类，位于`com.sun.org.apache.bcel.internal.classfile.AccessFlags`位置.这个类是所有对象的超类.

这个对应于类AccessFlags.这个类的属性`access_flag`表示权限属性.其中可选的值如下:

| 权限符           | 权限值 |
| ---------------- | ------ |
| ACC_PUBLIC       | 0x0001 |
| ACC_PRIVATE      | 0x0002 |
| ACC_PROTECTED    | 0x0004 |
| ACC_STATIC       | 0x0008 |
| ACC_FINAL        | 0x0010 |
| ACC_SYNCHRONIZED | 0x0020 |
| ACC_VOLATILE     | 0x0040 |
| ACC_TRANSIENT    | 0x0080 |
| ACC_NATIVE       | 0x0100 |
| ACC_INTERFACE    | 0x0200 |
| ACC_ABSTRACT     | 0x0400 |
| ACC_STRICT       | 0x0800 |

#### 属性处理

JVM对属性的处理是`com.sun.org.apache.bcel.internal.classfile.Attribute`类.

这个是所有**属性**对象的超类.当前支持`ConstantValue`,`SourceFile`,`Code`,`Exceptiontable`,`LineNumberTable`,`LocalVariableTable`,`InnerClasses`,`Synthetic`类型.

##### 内部属性

| 属性名称      | 解释                   |
| ------------- | ---------------------- |
| name_index    | 指向常量池中的属性名称 |
| length        | 属性的长度             |
| tag           | 分辨子类的标记         |
| constant_pool | 常量池                 |

##### 读取属性值

jvm内部提供有输入流读取属性的方法.这个无法再外部系统使用,由`Field`,`Method`构造器调用

```java
public static final Attribute readAttribute(DataInputStream file,
                                              ConstantPool constant_pool)
    throws IOException, ClassFormatException
  {
    ConstantUtf8 c;
    String       name;
    int          name_index;
    int          length;
    byte         tag = Constants.ATTR_UNKNOWN; // Unknown attribute

	// 通过name_index蚕食间接的从常量池中获取类名
    name_index = (int)file.readUnsignedShort(); // 反序列化索引位置
    // 获取指定索引的常量池中的值
    c          = (ConstantUtf8)constant_pool.getConstant(name_index,
                                                         Constants.CONSTANT_Utf8);
    name       = c.getBytes();

    // 字节数据的长度
    length = file.readInt();

	
    // 获取属性的名称,这里记住属性列表的索引值即可,可以通过下面参数索引
    /**
        "SourceFile", "ConstantValue", "Code", "Exceptions",
        "LineNumberTable", "LocalVariableTable",
        "InnerClasses", "Synthetic", "Deprecated",
        "PMGClass", "Signature", "StackMap",
        "LocalVariableTypeTable"
    */
    for(byte i=0; i < Constants.KNOWN_ATTRIBUTES; i++) {
      if(name.equals(Constants.ATTRIBUTE_NAMES[i])) {
        tag = i; // found!
        break;
      }
    }

    // 使用模式匹配调用对应的构造器,创建对应的属性
    switch(tag) {
    case Constants.ATTR_UNKNOWN:
      AttributeReader r = (AttributeReader)readers.get(name);

      if(r != null)
        return r.createAttribute(name_index, length, file, constant_pool);
      else
        return new Unknown(name_index, length, file, constant_pool);

    case Constants.ATTR_CONSTANT_VALUE:
      return new ConstantValue(name_index, length, file, constant_pool);

    case Constants.ATTR_SOURCE_FILE:
      return new SourceFile(name_index, length, file, constant_pool);

    case Constants.ATTR_CODE:
      return new Code(name_index, length, file, constant_pool);

    case Constants.ATTR_EXCEPTIONS:
      return new ExceptionTable(name_index, length, file, constant_pool);

    case Constants.ATTR_LINE_NUMBER_TABLE:
      return new LineNumberTable(name_index, length, file, constant_pool);

    case Constants.ATTR_LOCAL_VARIABLE_TABLE:
      return new LocalVariableTable(name_index, length, file, constant_pool);

    case Constants.ATTR_LOCAL_VARIABLE_TYPE_TABLE:
      return new LocalVariableTypeTable(name_index, length, file, constant_pool);

    case Constants.ATTR_INNER_CLASSES:
      return new InnerClasses(name_index, length, file, constant_pool);

    case Constants.ATTR_SYNTHETIC:
      return new Synthetic(name_index, length, file, constant_pool);

    case Constants.ATTR_DEPRECATED:
      return new Deprecated(name_index, length, file, constant_pool);

    case Constants.ATTR_PMG:
      return new PMGClass(name_index, length, file, constant_pool);

    case Constants.ATTR_SIGNATURE:
      return new Signature(name_index, length, file, constant_pool);

    case Constants.ATTR_STACK_MAP:
      return new StackMap(name_index, length, file, constant_pool);

    default: // Never reached
      throw new IllegalStateException("Ooops! default case reached.");
    }
  }
```

#### 类转换功能

这个类位于`com.sun.org.apache.bcel.internal.classfile.ClassParser`,这个类可以转换给定的`.class`文件.方法`parse`在执行成功的时候会返回一个java类.当出现io异常或者传输不一致的情况,就会失败.

##### 属性介绍

| 属性名称                                | 类型              | 介绍              |
| --------------------------------------- | ----------------- | ----------------- |
| file                                    | *DataInputStream* | 输入文件流        |
| zip                                     | *ZipFile*         | zip文件           |
| file_name                               | *String*          | 文件名称          |
| class_name_index, superclass_name_index | *int*             | 类/超类名称索引   |
| major, minor                            | int               | 编译器版本        |
| access_flags                            | int               | 权限标识符        |
| interfaces                              | *int*[]           | 实现接口列表      |
| constant_pool                           | *ConstantPool*    | 常量池            |
| fields                                  | *Field*[]         | 类属性列表        |
| methods                                 | *Method*[]        | 类方法列表        |
| attributes                              | *Attribute*[]     | 类属性列表        |
| is_zip                                  | *boolean*         | 是否由zip加载文件 |
| BUFSIZE                                 | int               | 缓冲区大小(8192)  |

##### 转换功能

转换给定的类文件,并返回一个对象.包含其中的数据,常量,方法,属性和指令等.如果不满足`.class`的格式要求,这时候会抛出`ClassFormatException `异常.

```java
public JavaClass parse() throws IOException, ClassFormatException
  {
    // 读取class文件头部信息,先检查class文件的魔数信息
    readID();

	// 读取编译器版本
    readVersion();

    // 读取常量池信息
    readConstantPool();

    // 读取类的信息
    readClassInfo();

	// 读取接口信息(例如,实现的接口信息)
    readInterfaces();

	// 读取类属性和方法
	// 读取类属性,例如类的变量
    readFields();

	// 读取类的方法,例如,类的函数
    readMethods();

	// 读取类的属性
    readAttributes();

    // Check for unknown variables
    //Unknown[] u = Unknown.getUnknownAttributes();
    //for(int i=0; i < u.length; i++)
    //  System.err.println("WARNING: " + u[i]);

    // Everything should have been read now
    //      if(file.available() > 0) {
    //        int bytes = file.available();
    //        byte[] buf = new byte[bytes];
    //        file.read(buf);

    //        if(!(is_zip && (buf.length == 1))) {
    //          System.err.println("WARNING: Trailing garbage at end of " + file_name);
    //          System.err.println(bytes + " extra bytes: " + Utility.toHexString(buf));
    //        }
    //      }

    // 读取其他
    file.close();
    if(zip != null)
      zip.close();

	// 返回具有上述信息的java类
    return new JavaClass(class_name_index, superclass_name_index,
                         file_name, major, minor, access_flags,
                         constant_pool, interfaces, fields,
                         methods, attributes, is_zip? JavaClass.ZIP : JavaClass.FILE);
  }
```

#### 代码块

JVM对于代码块的描述位于`com.sun.org.apache.bcel.internal.classfile.Code`中.这个类代表一个方法中的字节码代码块.由方法Attribute.readAttribute()进行实例化。一个代码属性包含**操作栈**,**本地变量**,**字节码**和**异常处理**组成.

这个属性有自己的属性,叫做**行号表**LineNumberTable,这个是用于debug设计出来的,且**本地变量表**中包含了本地变量的信息.

| 属性名称               | 介绍                      |
| ---------------------- | ------------------------- |
| max_stack              | 方法最大的栈深度          |
| max_locals             | 本地变量的数量            |
| code_length            | 代码长度(字节为单位)      |
| code                   | 实际的代码                |
| exception_table_length | 异常表长度                |
| exception_table        | 异常表                    |
| attributes_count       | 属性计数器(代码属性:行号) |
| attributes             | 本地变量表                |

#### 代码异常处理

JVM对于代码块的异常处理位于`com.sun.org.apache.bcel.internal.classfile.CodeException`

| 属性名称   | 介绍                                                         |
| ---------- | ------------------------------------------------------------ |
| start_pc   | 异常处理器在代码中的范围                                     |
| end_pc     | 异常处理器结束行号                                           |
| handler_pc | 异常处理器起始地址,例如,代码的起始偏移量                     |
| catch_type | 如果这个值为0,则异常处理器会捕获任何的异常,否则会执行捕捉的异常类 |

#### 常量处理

JVM对于常量处理位于`com.sun.org.apache.bcel.internal.classfile.Constant`位置

这个类是常量类的抽象类

##### 常量值的读取

```java
// 从给定的文件中读取一个常量,类型参数依赖于一个标签字节
static final Constant readConstant(DataInputStream file)
    throws IOException, ClassFormatException
  {
    byte b = file.readByte(); // Read tag byte

    switch(b) {
    case Constants.CONSTANT_Class:              return new ConstantClass(file);
    case Constants.CONSTANT_Fieldref:           return new ConstantFieldref(file);
    case Constants.CONSTANT_Methodref:          return new ConstantMethodref(file);
    case Constants.CONSTANT_InterfaceMethodref: return new
                                        ConstantInterfaceMethodref(file);
    case Constants.CONSTANT_String:             return new ConstantString(file);
    case Constants.CONSTANT_Integer:            return new ConstantInteger(file);
    case Constants.CONSTANT_Float:              return new ConstantFloat(file);
    case Constants.CONSTANT_Long:               return new ConstantLong(file);
    case Constants.CONSTANT_Double:             return new ConstantDouble(file);
    case Constants.CONSTANT_NameAndType:        return new ConstantNameAndType(file);
    case Constants.CONSTANT_Utf8:               return new ConstantUtf8(file);
    default:
      throw new ClassFormatException("Invalid byte tag in constant pool: " + b);
    }
  }
```

| 常量类型                    | 介绍                                |
| --------------------------- | ----------------------------------- |
| CONSTANT_Class              | 常量类                              |
| ConstantFieldref            | 代表指向常量池中属性的引用          |
| CONSTANT_Methodref          | 代表常量池方法的引用                |
| CONSTANT_InterfaceMethodref | 代表常量池接口方法的引用            |
| CONSTANT_String             | 代表指向常量池中字符串的引用        |
| CONSTANT_Integer            | 代表指向常量池中Integer类型的引用   |
| CONSTANT_Float              | 代表指向常量池中Float类型的引用     |
| CONSTANT_Long               | 代表指向常量池中Long类型的引用      |
| CONSTANT_Double             | 代表指向常量池中Double类型的引用    |
| CONSTANT_NameAndType        | 代表指向常量池中属性/方法的名称引用 |
| CONSTANT_Utf8               | 代表指向常量池中UTF-8字符串的引用   |

#### 常量池

这个类代表了常量池,例如常量表.可以包含空引用.

| 属性                | 介绍         |类型|
| ------------------- | ------------ |----|
| constant_pool_count | 常量池计数器 |int|
| constant_pool       | 常量池       |Constant[]|

##### *Constant* getConstant(*int* index)

获取常量池指定索引处的常量值

```java
public Constant getConstant(int index) {
    if (index >= constant_pool.length || index < 0)
        throw new ClassFormatException("Invalid constant pool reference: " +
                                       index + ". Constant pool size is: " +
                                       constant_pool.length);
    return constant_pool[index];
}
```

#####  *String* constantToString(*Constant* c)

常量池转string类型

```java
public String constantToString(Constant c)
    throws ClassFormatException
  {
    String   str; // 需要返回的字符串
    int      i; // 常量池指针
    byte     tag = c.getTag(); // 获取类型标签

    switch(tag) {
    case Constants.CONSTANT_Class:
      i   = ((ConstantClass)c).getNameIndex();
      c   = getConstant(i, Constants.CONSTANT_Utf8);
      str = Utility.compactClassName(((ConstantUtf8)c).getBytes(), false);
      break;

    case Constants.CONSTANT_String:
      i   = ((ConstantString)c).getStringIndex();
      c   = getConstant(i, Constants.CONSTANT_Utf8);
      str = "\"" + escape(((ConstantUtf8)c).getBytes()) + "\"";
      break;

    case Constants.CONSTANT_Utf8:    str = ((ConstantUtf8)c).getBytes();         break;
    case Constants.CONSTANT_Double:  str = "" + ((ConstantDouble)c).getBytes();  break;
    case Constants.CONSTANT_Float:   str = "" + ((ConstantFloat)c).getBytes();   break;
    case Constants.CONSTANT_Long:    str = "" + ((ConstantLong)c).getBytes();    break;
    case Constants.CONSTANT_Integer: str = "" + ((ConstantInteger)c).getBytes(); break;

    case Constants.CONSTANT_NameAndType:
      str = (constantToString(((ConstantNameAndType)c).getNameIndex(),
                              Constants.CONSTANT_Utf8) + " " +
             constantToString(((ConstantNameAndType)c).getSignatureIndex(),
                              Constants.CONSTANT_Utf8));
      break;

    case Constants.CONSTANT_InterfaceMethodref: case Constants.CONSTANT_Methodref:
    case Constants.CONSTANT_Fieldref:
      str = (constantToString(((ConstantCP)c).getClassIndex(),
                              Constants.CONSTANT_Class) + "." +
             constantToString(((ConstantCP)c).getNameAndTypeIndex(),
                              Constants.CONSTANT_NameAndType));
      break;

    default: // Never reached
      throw new RuntimeException("Unknown constant type " + tag);
    }

    return str;
  }
```

#### 异常表

JVM对于这个类的实现位于`com.sun.org.apache.bcel.internal.classfile.ExceptionTable`,这个类代表方法抛出的异常表.每个方法只能使用一个属性.

| 属性                  | 类型           | 介绍  |
| --------------------- | -------------- | ----- |
| number_of_exceptions  | 异常数量       | int   |
| exception_index_table | 异常表(常量池) | int[] |

#### 属性/方法

JVM关于属性/方法的描述位于`com.sun.org.apache.bcel.internal.classfile.FieldOrMethod`,这个是方法和属性的抽象超类.

| 属性名称         | 类型         | 介绍               |
| ---------------- | ------------ | ------------------ |
| name_index       | int          | 指向常量池属性名称 |
| signature_index  | int          | 指向编码标志       |
| attributes_count | int          | 属性计数器         |
| attributes       | Attribute[]  | 属性集合           |
| constant_pool    | ConstantPool | 常量池             |

#### 内部类

| 属性名称           | 类型 | 介绍                       |
| ------------------ | ---- | -------------------------- |
| inner_class_index  | int  | 内部类在常量池中的位置     |
| outer_class_index  | int  | 外部类在常量池中的位置     |
| inner_name_index   | int  | 内部类名称在常量池中的位置 |
| inner_access_flags | int  | 内部类权限标识符           |

#### java类的描述

JVM关于java类的描述位于`com.sun.org.apache.bcel.internal.classfile.JavaClass`.这个代表了一个java类,例如,数据结构,常量池,属性,方法和指令.

##### 属性列表

| 属性名称              | 类型           | 介绍                                                         |
| --------------------- | -------------- | ------------------------------------------------------------ |
| file_name             | String         | 文件名称                                                     |
| package_name          | String         | 包名称                                                       |
| source_file_name      | String         | 源文件名称                                                   |
| class_name_index      | int            | 类名称在常量池中的索引                                       |
| superclass_name_index | int            | 超类在常量池中的名称索引                                     |
| class_name            | String         | 类名称                                                       |
| superclass_name       | String         | 超类名称                                                     |
| major, minor          | int            | 编译器版本                                                   |
| constant_pool         | ConstantPool   | 常量池                                                       |
| interfaces            | int[]          | 实现接口列表                                                 |
| interface_names       | String[]       | 接口名称列表                                                 |
| fields                | Field[]        | 类属性列表                                                   |
| methods               | Method[]       | 方法属性列表                                                 |
| attributes            | Attribute[]    | 属性列表                                                     |
| source                | byte           | 生成位置(默认heap --> 内存)<br />ZIP zip文件<br />FILE 以文件形式生成 |
| debug                 | boolean(false) | 是否处于debug模式                                            |
| sep                   | char           | 目录分隔符                                                   |



#### 行号处理

##### LineNumber

这个类代表了**程序计数器**和**行号**的关系.

###### 属性列表

| 属性        | 介绍               |
| ----------- | ------------------ |
| start_pc    | 行对应的程序计数器 |
| line_number | 行号               |

##### LineNumberTable

这个类代表了行号形成的表,主要用于debug.这个属性被**Code**使用,包含程序计数器和行号

| 属性                     | 介绍       |
| ------------------------ | ---------- |
| line_number_table_length | 行号表长度 |
| line_number_table        | 行号表     |

#### 本地变量相关

##### LocalVariable

这个方法代表了本地变量

| 属性            | 类型         | 介绍                     |
| --------------- | ------------ | ------------------------ |
| start_pc        | int          | 变量有效范围             |
| length          | int          | 本地变量长度             |
| name_index      | int          | 变量名称在常量池中的位置 |
| signature_index | int          | 变量签名的索引           |
| index           | int          | 变量索引位置             |
| constant_pool   | ConstantPool | 常量池                   |

##### LocalVariableTable

本地变量表,这个属性在**Code**中包含

| 属性                        | 介绍           |
| --------------------------- | -------------- |
| local_variable_table_length | 本地变量表长度 |
| local_variable_table        | 本地变量表     |

#### 程序访问者

JVM对于程序的调用者提供了一个类来处理,即`com.sun.org.apache.bcel.internal.classfile.Visitor`.这个类中提供了各类JVM参数的访问方法.

```java
public interface Visitor {
  public void visitCode(Code obj);
  public void visitCodeException(CodeException obj);
  public void visitConstantClass(ConstantClass obj);
  public void visitConstantDouble(ConstantDouble obj);
  public void visitConstantFieldref(ConstantFieldref obj);
  public void visitConstantFloat(ConstantFloat obj);
  public void visitConstantInteger(ConstantInteger obj);
  public void visitConstantInterfaceMethodref(ConstantInterfaceMethodref obj);
  public void visitConstantLong(ConstantLong obj);
  public void visitConstantMethodref(ConstantMethodref obj);
  public void visitConstantNameAndType(ConstantNameAndType obj);
  public void visitConstantPool(ConstantPool obj);
  public void visitConstantString(ConstantString obj);
  public void visitConstantUtf8(ConstantUtf8 obj);
  public void visitConstantValue(ConstantValue obj);
  public void visitDeprecated(Deprecated obj);
  public void visitExceptionTable(ExceptionTable obj);
  public void visitField(Field obj);
  public void visitInnerClass(InnerClass obj);
  public void visitInnerClasses(InnerClasses obj);
  public void visitJavaClass(JavaClass obj);
  public void visitLineNumber(LineNumber obj);
  public void visitLineNumberTable(LineNumberTable obj);
  public void visitLocalVariable(LocalVariable obj);
  public void visitLocalVariableTable(LocalVariableTable obj);
  public void visitLocalVariableTypeTable(LocalVariableTypeTable obj);
  public void visitMethod(Method obj);
  public void visitSignature(Signature obj);
  public void visitSourceFile(SourceFile obj);
  public void visitSynthetic(Synthetic obj);
  public void visitUnknown(Unknown obj);
  public void visitStackMap(StackMap obj);
  public void visitStackMapEntry(StackMapEntry obj);
}
```

