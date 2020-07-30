#### 程序架构

<img src="E:\截图文件\goroutine程序架构.png" style="zoom:67%;" />

执行的顺序是从主goroutine开始,一直到执行搜索的goroutine和跟踪结果的goroutine,最后回到主goroutine.

#### main包

程序的主入口在`main.go`文件中可以找到,例如:

```go
package main

// 导入的时候需要注意,如果导入的库没有被使用,则需要在库名称前面加上 _ 修饰符
import (
	"fmt"
	"log"
	_ "os"
)

func main() {
	fmt.Println("hello world!")
	log.Print("running successfully...")

}
```

注意到,`main`函数是用于构建可执行文件的,需要存在这个函数才能运行.

需要注意到的是,`main`函数必须在`main`包中,否则不能构建.从标准库中导入代码的时候,编译器会自动去`GOROOT`和`GOPATH`中去查找.启动IDE的时候,需要设置Go的环境变量

```properties
GOROOT=D://Go
GOPATH=E://GoLand
```

#### 变量的访问权限

Go语言中，标识符要么从包中公开，要么不从包中公开。当代码导入一个包的时候，程序可以直接访问任意一个公开的标识符，这些标识符以**大写字母**开头，小写字母开头是不公开的。(当然，可以通过间接的方式访问)。

#### 参数的默认值

数值类型默认值为0, 字符串类型默认值为空串, 布尔类型默认值为`false`. 对于指针, 零值为Nil. 对于引用类型来说, 底层数据结构会被设置为零值.

