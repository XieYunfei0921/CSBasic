#### Process介绍

```markdown
进程类:
提供本地进程的控制,这个进程可以使用ProcessBuilder.start和Runtime.exec来启动.
这个类提供进程的输入,和进程的输出,以及等待进程的完成,检查进程的退出状态,和撤销(kill)进程.
ProcessBuilder.start和Runtime.exec会创建一个本地进程,并返回一个进程子类的实例,这个实例可以控制进程,且获取到它的信息.
创建进程的方法不会对于指定平台的指定进程起效.
默认情况下,创建的进程不会持有自己的控制台.其标准IO操作会被重定向到父进程中,且可以使用流式方法获取.例如getOutputStream(),getInputStream()以及getErrorStream().
父进程使用这些流去获取进程输出.因为一些本地平台仅仅提供了有限的标准IO缓冲区大小,读写进程流会导致进程阻塞,甚至是死锁.如果需要,可以使用ProcessBuilder进行进程IO重定向.
当没有指向Process对象的引用的时候,进程不会被kill.而非进程一致连续执行.
```

#### 常见方法

##### abstract OutputStream getOutputStream()

返回连接到进程标准输入的输出流,输出数据到指定进程的标准输入中,可以使用redirectInput进行重定向,重定向的时候会返回空输出流

##### abstract InputStream getInputStream()
返回连接到当前进程的标准输出的输入流.这个流会从进程的标准输出获取数据.可以使用redirectOutput对输出进行重定向
##### abstract InputStream getErrorStream()
返回进程标准错误的输入流,这个流包含当前进程错误输出的数据.如果进程标准错误输出已经使用ProcessBuilder的redirectError方法进行重定向,就会返回一个空的输入流.
##### abstract int waitFor()
使当前线程等待,如果需要的情况下等待到进程结束.这个方法如果进程结束的时候会立即返回.如果还没有停止,会阻塞到进程结束
##### abstract int exitValue()
返回进程的退出值,为0时为正常退出
##### abstract void destroy()
进程的撤销

#### PipeInputStream介绍
子进程管道的输入流,可以跳读n个字节
```java
public long skip(long n) throws IOException {
    long remaining = n;
    int nr;
        if (n <= 0) {
        	return 0;
        }
    int size = (int)Math.min(2048, remaining);
    byte[] skipBuffer = new byte[size];
    while (remaining > 0) {
        nr = read(skipBuffer, 0, (int)Math.min(size, remaining));
        if (nr < 0) {
        	break;
        }
        remaining -= nr;
    }
    return n - remaining;
}
```

#### ProcessBuilder介绍

```markdown
这个类用于创建操作系统进程
每个进程构建器@ProcessBuilder 实例都会管理进程属性的集合.start方法会使用指定数据创建进程实例.可以使用相同的实例重复调用,
用于创建表示的子进程.如果调用了startPipeline方法创建进程的pipeline。会发送每个进程的输出到下个进程中。每个进程有各自的配置。进程构建器管理如下属性:

 * command:
字符串列表,用于指定外部程序文件和相关参数,如果有的话,字符串列表代表合法的操作系统指令是具有系统独立性的.例如,对于列表中每个概念上的参数都是合理的,但是有一些操作系统指令,程序需要获取授权才行.在这些系统上,java实现需要指令包含两类参数

 * environment:
具有系统独立性的变量映射,初始值是当前进程环境变量的副本.

 * working directory:
默认值为当前进程的当前工作目录,通常为系统属性值,user.dir

 * standard input:
默认情况下,子进程读取管道输入,java代码可以通过@getOutputStream的输出流获取管道数据.但是标准输出可以重定向到其他资源中,使用@redirectInput.这种情况下,@getOutputStream会返回一个空的输出流.

 * standard output
默认情况下,子进程写出标准输出和标准错误到这个管道中.java代码可以通过@getOutputStream或@getErrorStream获取通道中的世界.但是,标准输出和标准错误可能重定向到其他位置.这样就会返回空的输入流.

 * redirectErrorStream
这个属性默认为false,意味着标准输出和标准错误的输出会发送到两个独立的流中,这个可以使用@getInputStream或者@getErrorStream获取.设置为true之后:
标准错误会被合并到标准输出中,且总是发送到目标位置中.如果重定向则会返回null.修改这里的属性会影响到下一个启动的进程,但是不会影响到之前启动的进程和自身进程.start方法会进程错误检查,如果不通过会失败.注意这个类不是同步的,如果多个线程并发的获取这个类,必须要外部同步,使得至多一个线程能够修改.启动进程的方式很简单
Process p = new ProcessBuilder("myCommand", "myArg").start();
下面启动一个进程,修改工作目录和环境变量,且重定向标准输出和标准错误,用于添加日志文件.
 使用显示环境变量集启动进程的时候,添加环境变量前调用Map.clear(),清除之前的环境变量.
```

使用示例

```java
ProcessBuilder pb =new ProcessBuilder("myCommand", "myArg1", "myArg2");
Map<String, String> env = pb.environment();
env.put("VAR1", "myValue");
env.remove("OTHERVAR");
env.put("VAR2", env.get("VAR1") + "suffix");
pb.directory(new File("myDir"));
File log = new File("log");
pb.redirectErrorStream(true);
pb.redirectOutput(Redirect.appendTo(log));
Process p = pb.start();
assert pb.redirectInput() == Redirect.PIPE;
assert pb.redirectOutput().file() == log;
assert p.getInputStream().read() == -1;
```

#### ProcessBuilder属性

```markdown
1. List<String> command
指令集

2. File directory
目录

3. Map<String,String> environment
环境变量列表

4. boolean redirectErrorStream
是否重定向错误流

5. Redirect[] redirects
重定向列表
```

#### ProcessBuilder常见方法

##### public Map<String,String> environment()

返回线程构建器的环境变量列表,无论进程合适构建,环境变量都会当前进程环境变量的副本.子进程会使用父进程的环境变量.

##### Redirect from(final File file)

来自指定文件的重定向读取

##### Redirect to(final File file)

重定向写出到指定文件中

##### Redirect appendTo(final File file) 

重定向添加到指定文件中

