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

#### go vet指令

使用`go vet`指令可以捕获下述错误

1. `Printf`类函数调用的时候, 类型匹配错误的参数
2. 定义常用的方法时, 方法签名的错误
3. 错误的结构标签
4. 没有指定字段名的结构字面量

例如

```go
package main

func main() {
	fmt.Printf("lazy dogs",3.14)
}
```

这里执行的时候,控制台输出就存在问题

```shell
lazy dogs%!(EXTRA float64=3.14)
```

使用`go vet main.go`时出现如下的信息

```shell
.\Hello.go:13:12: Printf call has arguments but no formatting directives
```

#### 格式整理

使用`go fmt`可以对go代码进行格式的整理

例如

```go
var err error = nil
if err!=nil { fmt.Println(err.Error()) }
```

整理完毕之后得到

```go
var err error = nil
if err != nil {
    fmt.Println(err.Error())
}
```

可以方便的对格式进行整理

#### Go语言的文档

##### 命令行方式获取文档

例如如果需要浏览Unix查看`tar`的文档,只需要执行`go doc tar`即可

```shell
$ go doc tar

package tar // import "archive/tar"

Package tar implements access to tar archives.

Tape archives (tar) are a file format for storing a sequence of files that
can be read and written in a streaming manner. This package aims to cover
most variations of the format, including those produced by GNU and BSD tar
tools.

const TypeReg = '0' ...
var ErrHeader = errors.New("archive/tar: invalid tar header") ...
type Format int
    const FormatUnknown Format ...
type Header struct{ ... }
    func FileInfoHeader(fi os.FileInfo, link string) (*Header, error)
type Reader struct{ ... }
    func NewReader(r io.Reader) *Reader
type Writer struct{ ... }
    func NewWriter(w io.Writer) *Writer
```

##### web方式浏览文档

执行`godoc -http=:${port}`即可

#### 依赖管理

使用`godep`和`vendor`使用三方工具导入路径重写,解决了依赖的问题. 主要是通过所有的依赖包复制到代码库中实现, 然后使用工程内部依赖包所在目录重写所有的导入路径.

导入路径之前, import语句导入的是正常的包

```go
package main
import (
 	"bitbucket.org/ww/goautoneg"
 	"github.com/beorn7/perks"
)
```

包所对应的代码存放在`GOPATH`所指定的磁盘目录上面, 在依赖管理之后, 导入路径需要重写成工程内部依赖包的路径. 

```go
package main
import (
	"github.ardanstudios.com/myproject/Godeps/_workspace/src/bitbucket.org/ww/goautoneg"
	"github.ardanstudios.com/myproject/Godeps/_workspace/src/github.com/beorn7/perks"
)
```

这样包不易于使用, 但是可以更加方便的重构工程. 另一个好处是支持使用`go get`指令获取代码库, 当获取这个工厂的代码库的时候, `go get`可以找到每个包, 并保证位置的正确性.

#### 构建工具gb

gb是一种新的构建工具, gb基于工程将go工具链工作空间的元信息作为替换. 这种方案不需要重写代码内导入路径, 且导入路径仍旧可以通过`go get`来获取.

是以gb的导入路径如下

```go
package main
import (
 	"bitbucket.org/ww/goautoneg"
 	"github.com/beorn7/perks"
)
```

gb工具首先会在`src`目录下面查找代码,如果查找不到,会到`vender/src/`下面去查找代码.

gb工程的构建

```shell
$ gb build all
```

