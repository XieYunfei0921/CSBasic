#### **Docker应用开发**

---

#### 应用开发综述

1.  在Docker上开发新的应用

   如果你刚开始使用docker，需要了解如下知识是你更好地理解Docker

   +  使用Dockerfile构建镜像 https://docs.docker.com/get-started/part2/

   +  使用多段构建，保证你的镜像依赖，参考[多段构建](# https://docs.docker.com/develop/develop-images/multistage-build/)

   +  使用**数据卷**和**绑定文件系统**管理应用数据

     1.  [数据卷](# https://docs.docker.com/storage/volumes/)
     2.  [绑定文件系统](# https://docs.docker.com/engine/admin/volumes/bind-mounts/)

   +  使用k8s扩展应用 

     [扩展方法](# https://docs.docker.com/get-started/part3/)

   +  使用swarm扩展应用

     [扩展方法](# https://docs.docker.com/get-started/part4/)

   +  应用开发最佳实践

2.  学习如何使用Docker开发指定语言的应用

   +  [java开发](# https://github.com/docker/labs/tree/master/developer-tools/java/)
   + [node.js开发](# https://github.com/docker/labs/tree/master/developer-tools/nodejs/porting)

3.  使用SDK或API进行高级开发

   学会如何去写dockerfile和构建文件且会使用docker客户端之后,就可以使用docker引擎SDK(go/python)或者直接使用HTTP的api.

#### 应用开发最佳实践

1.  保证镜像尽可能的小

   启动容器和服务时,小的镜像可以快速从网络上拉取以及快速的载入内存.下述集中准则可以是镜像比较小.

   + 使用精确的基础镜像

     例如,如果你需要jdk,只需要官方的`openjdk`镜像即可,不需要先获取一个`ubuntu`,使`openjdk`作为`ubuntu`的一部分存在.

   +  使用多段构建

     例如,你可以使用`maven`镜像去构建java应用,然后重置`tomcat`镜像.并拷贝java组件到应用的中的正确位置.所有信息都在同一个docekrfile中,这就意味着最终镜像不会包含所有构建过程中产生的库和依赖.

     1.  如果你需要使用不包含多段构建的docker,试着降低镜像的层数,主要方法是通过`RUN`将不同的阶段分开.可以通过合并这些`RUN`指令到单行当中.下面的例子中,上一个存在有两层,下面的只有一层.

        ```shell
        RUN apt-get -y update
        RUN apt-get install -y python
        ```

        ```shell
        RUN apt-get -y update && apt-get install -y python
        ```

     2.  如果你有多个通用镜像,那么这个时候考虑到使用共享组件创建自己的镜像.,此时就会基于你的自定义镜像进行开发.docker需要一次加载通用层,且它们都被缓存了.这就意味着你能够获取镜像更加的快速和高效.

     3.  保证生成出来的镜像尽可能小,但是需要运行debug.考虑到使用生成镜像作为基础镜像,且需要包含debug镜像.因此,额外的debug工具就需要添加到生成镜像中.

     4.  当前组件镜像时,总是使用标注版本信息,使用功能(prod或test),稳定与否的标签去对其进行标签.不能依赖自动创建的`lastest`标签.

2. 应用数据的持久化

   +  使用存储驱动器避免容器可写层存储应用数据。者增加了你的容器大小，使其IO更加低效。
   +  相反，使用数据卷来存储数据
   +  开发期间，需要使用数据装置为止时，当你需要在容器上装置资源目录时。对于生产情况下，使用数据卷去存储数据。将其装置在开发期间装置的位置处。
   +  生产环境下，使用[**密钥**](https://docs.docker.com/engine/swarm/secrets/)去存储服务敏感应用数据.在配置文件中使用非敏感数据的配置.如果当前你处于脱机状态下,考虑去使用单副本服务,这样就可以利用只服务的特征.

3. 使用CI/CDE进行测试和部署

   +  当你检查到资源控制或者创建PR的变化时.使用DockHub或者其他CI/CD pipeline去自动构建,并对镜像镜像标记测试.
   +  通过设置开发环境,进一步的设置安全,测试配置,用于在部署到生成环境之前对镜像进行标记.这种方式下,你可以在部署之前确认,通过环境,安全组信息对其进行检查.

4. 不同的部署方式和生产环境

   | 开发环境               | 生产环境                                                     |
   | ---------------------- | ------------------------------------------------------------ |
   | 通过代码中设置绑定配置 | 使用数据卷存储数据                                           |
   | 使用DockerDestop       | 使用Docker引擎,使用用户映射关键去增强docker经常的隔离性      |
   | 不用在意时间漂移       | 在Docker主机上使用NTP客户端,在容器进程内部将其同步到NTP服务器上. |

#### 镜像开发

1. Dockfile最佳实践

   这个教程覆盖了构建高效镜像的内容.

   docker允许读取结构化的dockerfile(包含所有的指令)来自动构建镜像.一个Docker文件可以及程序指定的格式以及一系列的[操作指令](https://docs.docker.com/engine/reference/builder/)

   docker镜像包含只读层,每层代表一个dockerfile结构体.层直接相互堆放,每一个都是之前层次的编号.参照下面的`dockerfile`

   ```dockerfile
   FROM ubuntu:18.04
   COPY . /app
   RUN make /app
   CMD python /app/app.py
   ```

   每个指令都创建了一个层

   +  `FROM` 创建了一个`ubuntu:18.04` docker镜像
   +  `COPY` 添加来自于docker客户端当前目录的文件
   +  `RUN` 使用make构建你的应用
   +  `CMD` 只读在容器内运行的指令

   当你运行一个镜像且生成一个容器时,在底层上面添加一个新的*可写层*.所有对运行容器的变化,比如说写新的文件,修改存在的文件,删除文件,被写到可写容器层.

   获取更多的镜像层(和docker如何构建和存储镜像),请参考[存储驱动器](https://docs.docker.com/storage/storagedriver/)

   +  通用指导

     1.  创建临时容器

        镜像中生成的容器需要尽可能的具有临时性.这就意味着容器可以停止和被销毁.可以使用绝对最小值来常见和配置,使其能够重建和替换.

        参考在12个因素应用方法区获取无状态下运行容器的体验,请[参考](https://12factor.net/processes)

     2.  立即构建上下文

        提交`docker build`指令是,当前工作目录叫做*构建上下文*,默认情况下,dockerfile是就地运行的.你可以通过(-f)来指定不同的位置.无论`dockerfile`在何处运行,当前目录的递归文件和目录都会被返送到docker启动器中,作为构建上下文存在.

        > **构建示例**:
        >
        > 创建目录用于构建上下文,使用`cd`进入目录,写入hello到`hello`中.创建docker文件,在上面运行`cat`.构建当前目录的上下文
        >
        > ```shell
        > mkdir myproject && cd myproject
        > echo "hello" > hello
        > echo -e "FROM busybox\nCOPY /hello /\nRUN cat /hello" > Dockerfile
        > docker build -t helloapp:v1 .
        > ```
        >
        > 移动`dockerfile`和`hello`到不同的目录,创建第二个版本的镜像.使用`-f`指定docker文件的目录,创建构建上下文.
        >
        > ```shell
        > mkdir -p dockerfiles context
        > mv Dockerfile dockerfiles && mv hello context
        > docker build --no-cache -t helloapp:v2 -f dockerfiles/Dockerfile context
        > ```

     在一个较大的**构建上下文**或者镜像中,包含文件信息不是必要的选择.这个可能导致构建时间的增长,拉取时间的增长,使得运行容器增大.下面提供**构建上文件**的大小度量信息:

     ```shell
     Sending build context to Docker daemon  187.8MB
     ```

   +  通过标准输入`stdin`导入dockerfile

     可以通过标准输入在本地或者远程构建上下文.使用标准输入导入配置可以不经过将`dockerfile`写出到磁盘的过程.在这种情况下,`dockerfile`不会持久化.

     > 下述两个指令执行效果相同:
     >
     > ```shell
     > echo -e 'FROM busybox\nRUN echo "hello world"' | docker build -
     > ```
     >
     > ```shell
     > docker build -<<EOF
     > FROM busybox
     > RUN echo "hello world"
     > EOF
     > ```
     >
     > 你可以选择提交方式

   + 使用标准输入构建镜像(不会发送**构建上下文**)

     使用从`stdin` 构建的`dockerfile`.不去发送额外的文件(**构建上下文**).连字符号(-)占用路径`PATH`的位置,知道docker读取**构建上下文**.

     ```shell
     docker build [OPTIONS] -
     ```

     下述例子时使用标准输入构建的`dockerfile`

     ```shell
     # 注意镜像名称必须要小写,否则名称不合法
     docker build -t myimage:latest -<<EOF
     
     FROM busybox
     RUN echo "hello world"
     EOF
     ```

     当前不需要文件拷贝到镜像中的时候,这种方式是非常有用的.可以提示构建速度,因为没有文件会参与启动过程.当然,如果你想自己只懂一些不参与构建的文件,请参考下述示例:

     > ```shell
     > # create a directory to work in
     > mkdir example
     > cd example
     > 
     > # create an example file
     > touch somefile.txt
     > 
     > docker build -t myimage:latest -<<EOF
     > FROM busybox
     > COPY somefile.txt .
     > RUN cat /somefile.txt
     > EOF
     > 
     > # observe that the build fails
     > ...
     > Step 2/3 : COPY somefile.txt .
     > COPY failed: stat /var/lib/docker/tmp/docker-builder249218248/somefile.txt: no such file or directory
     > ```

   + 使用标准输入从远端**构建上下文**

     从远端git仓库中使用`-f`指定需要使用的`dockerfile`使用连字符号去指定docekr去读取标准输入,格式参考如下:

     ```shell
     docker build [OPTIONS] -f- PATH
     ```

     当你需要一个不包含指定`dockerfile`的镜像时,或者希望构建一个自定义`dockerfile`,而不位置当前fork的仓库时.

     下述案例是使用标准输入添加从github仓库的`hello.c`到镜像中的指令:

     ```shell
     docker build -t myimage:latest -f- https://github.com/docker-library/hello-world.git <<EOF
     
     FROM busybox
     COPY hello.c .
     EOF
     ```

     >在底层执行中,当使用远程git仓库构建时,docekr进行了`git clone`指令,并将返送文件到执行结构上.这个过程需要安装`git`.

   +  使用`.dockerignore`排除指定文件

     类似`.gitignore`设置排除文件,请[参考](https://docs.docker.com/engine/reference/builder/#dockerignore-file)

   +  使用多段构建

     多段构建可以减小最终镜像的大小,不会减少立即层数和文件的数量.

     因为镜像在构建的最后阶段构建.可以通过平衡构建缓存来减少镜像的层数.

     例如,当你需要构建多个层的时候,你可以按照频率对其镜像排序,总体排序如下:

     + 安装你需要安装的工具
     + 安装/更新库依赖
     + 生成应用

     使用go应用的dockerfile可以参考下面示例:

     ```dockerfile
     FROM golang:1.11-alpine AS build
     
     # Install tools required for project
     # Run `docker build --no-cache .` to update dependencies
     RUN apk add --no-cache git
     RUN go get github.com/golang/dep/cmd/dep
     
     # List project dependencies with Gopkg.toml and Gopkg.lock
     # These layers are only re-built when Gopkg files are updated
     COPY Gopkg.lock Gopkg.toml /go/src/project/
     WORKDIR /go/src/project/
     # Install library dependencies
     RUN dep ensure -vendor-only
     
     # Copy the entire project and build it
     # This layer is rebuilt when a file changes in the project directory
     COPY . /go/src/project/
     RUN go build -o /bin/project
     
     # This results in a single layer image
     FROM scratch
     COPY --from=build /bin/project /bin/project
     ENTRYPOINT ["/bin/project"]
     CMD ["--help"]
     ```

   + 不要安装不必要的包

     为了降低复杂度,依赖和文件的大小,构建的次数,避免安装额外或者不必要的安装包(可能是最好安装的类型).比如,不需要在数据库镜像中安装文本编辑器.

   +  应用解耦合

     每个容器需要仅仅一个考虑,解耦合应用到多个容器中,使之更方便的水平扩展,和容器重用.例如,一个web应用,可能包含3个分开的容器,每个都有唯一的镜像,这个用于以解耦合的方式管理web应用,数据库,和内存缓存.

     限制每个容器到一个进程中是一个好的解决方式,但是不是一个快速的方式.例如,容器不仅能够使用初始进行进行繁衍,溢写程序还有繁衍出其他经常.例如,`Celery`可以扩展多个worker进程,而且`Apache`可以为每个请求创建一个进程.

     使用最好的判断,去保持容器竟可能的模块化.如果容器之间像话依赖,使用`Docker container network`去确保这些容器之间进行交流.

   + 层数最小化

     老版本的docker中，去缩小镜像的层数是很重要的，用于去保证他们的性能。下述特征将会添加，用于减小这个限制

     1.  仅仅`RUN` `COPY` 和 `ADD` 可以创建层,其他资料创建的临时的镜像,不会增加构建镜像的大小.
     2.  在有可能的地方使用多段构建,紧急拷贝你需要的版本到最终镜像中.**允许你器添加工具和debug信息在临时构建的阶段**,而不会增加最终镜像的大小.

   + 多行参数的排序

     可能的时候,通过对导航参数进行排序,这个帮助去避免包的复制,且使得列表易于更新.也使得PR更简单的去读取和检查.添加一个反斜杠帮助区分.比如

     ```shell
     RUN apt-get update && apt-get install -y \
       bzr \
       cvs \
       git \
       mercurial \
       subversion
     ```

   +   平衡构建缓存

      当构建一个镜像时,docekr通过`dockerfile`的指令对其进行构建.docekr在缓存中寻找一个存在的镜像,用于重用,而不是创建一个新的副本进行.

      如果你根本不想使用缓存,使用`-no-cache=true`,在`docekr build`中指定,但是,荣国你允许docekr使用缓存.name就需要明白什么时候缓存会使用.寻找匹配的镜像,基本准则如下:

      1.  父镜像已经处于缓存中,下一个指令用于去匹配所有子镜像,来源于这个基本镜像(父镜像).然后检查子镜像中是否有某个镜像需要其他的额外的指令,如果没有,那么这个缓存就是无效的.
      2.  在大多数情况下,和一个子镜像简单的比较`dockerfile`中的指令时有效的.但是,指定指令需要跟多的检查和解释.
      3.  对`ADD`和`COPY` 指令,镜像文件内容被检测,并生成**校验码**,最优一次修改和最新获取次数的信息不会参与校验码的计算.在缓存查找的时候,校验码会与当前存在的镜像校验码做比较.如果文件内容有编号(比如说文本内容和元数据信息的变化),那么该缓存会失效.
      4.  除了`ADD`和`COPY`指令之外,缓存检查不会检查容器内部的文件.例如,运行`RUN apt-get -y update` 指令,指令内容是更新容器内部的文件,但是不检查缓存是否命中.在这种情况下,这个指令串自己用于**模式匹配**.

      当缓存失效的时候,后来的`dockerfile`会生成新的镜像,且这个缓存不会被使用.

   +   Docker基础指令

      1.  [FROM 指令](https://docs.docker.com/engine/reference/builder/#from)

         在可能的情况下,当前官方镜像作为你的基础镜像,这里推荐[Alphine 镜像](https://hub.docker.com/_/alpine/)作为轻量控制,这个镜像大小比较小,同时提供了Linux环境.

      2.  [LABEL指令](https://docs.docker.com/config/labels-custom-metadata/)

         你可以对镜像添加标签,用于标识你的项目,记录发行信息.对于每个标签来说,添加标签以`LABEL`开始,且携带有一个或者多个键值对.下述例子显示了不同的格式,注释内容也做出标识.

         ```dockerfile
         # 设置一个或者多个标签
         LABEL com.example.version="0.0.1-beta"
         LABEL vendor1="ACME Incorporated"
         LABEL vendor2=ZENITH\ Incorporated
         LABEL com.example.release-date="2015-02-12"
         LABEL com.example.version.is-production=""
         ```

         一个镜像可以拥有多个标签,在docekr 1.10之前建议合并标签到一个`LABEL`指令中.主要是为了阻止创建其他镜像层.之后版本没这个必要,但是依旧支持合并.例如:

         ```dockerfile
         # 单行设置多个标签
         LABEL com.example.version="0.0.1-beta" com.example.release-date="2015-02-12"
         ```

         上述指令可以使用多行参数改写为

         ```dockerfile
         LABEL vendor=ACME\ Incorporated \
               com.example.is-beta= \
               com.example.is-production="" \
               com.example.version="0.0.1-beta" \
               com.example.release-date="2015-02-12"
         ```

         这里是设置对象标签接受kv对的[原理](https://docs.docker.com/config/labels-custom-metadata/),对于查询标签的信息,参考[类标签的管理](https://docs.docker.com/config/labels-custom-metadata/)

      3.  [RUN指令](https://docs.docker.com/engine/reference/builder/#run)

         使用多行指令使得长的`RUN`指令变得可读性更强

      4.  APT-GET指令

         `RUN`指令最常用执行的就是`apt-get`,因为他是包安装处理指令.使用时尽量避免使用`RUN apt-get upgrade`和`dit-upgrade`指令.因为许多父镜像的基础包不会更新.如果包内包含父镜像的过去包.联系他的维护者.如果你知道存在一个特定的包`foo`,那么可以直接指定安装`apt-get install -y foo`去自动更新.

         `RUN`指令中更新和安装同时使用,避免镜像中内容过期问题.

         ```dockerfile
         RUN apt-get update && apt-get install -y \
             package-bar \
             package-baz \
             package-foo
         ```

         单独使用更新指令可能会导致缓存出现问题,且使得安装指令出错.例如

         ```dockerfile
         FROM ubuntu:18.04
         RUN apt-get update
         RUN apt-get install -y curl
         ```

         构建镜像完成以及docker缓存中所有的层次后,建议去通过添加额外的包去修改添加额外的包.例如:

         ```dockerfile
         FROM ubuntu:18.04
         RUN apt-get update
         RUN apt-get install -y curl nginx
         ```

         docker将初始化和修改的指令作为标志,且重用之前步骤生成的缓存.因此更新指令因为作为缓存的原因没有执行.因为更新指令没有运行,构建肯获取多起的`curl`和`nginx`包.

         使用联合指令`RUN apt-get update && apt-get install -y`来确保docker按照最新的包文件.这项技术叫做**缓存失败**.可以通过指定包的版本达到缓存失败的效果.例如:

         ```dockerfile
         RUN apt-get update && apt-get install -y \
             package-bar \
             package-baz \
             package-foo=1.3.*
         ```

         **版本标识**使得构建去检索一个特定版本,从而无视缓存的存在.这项技术会降低获取安装包过程中出现的以外失败可能性.

         下面时一个`RUN`指令构建的标准案例:

         ```docker
         RUN apt-get update && apt-get install -y \
             aufs-tools \
             automake \
             build-essential \
             curl \
             dpkg-sig \
             libcap-dev \
             libsqlite3-dev \
             mercurial \
             reprepro \
             ruby1.9.1 \
             ruby1.9.1-dev \
             s3cmd=1.1.* \
          && rm -rf /var/lib/apt/lists/*
         ```

         例如,`s3cmd`指定版本为`1.1.*`.如果镜像用在老版本上,指定一个行的版本,会导致缓存失败.可以确保安装到新的版本.使用多行命令格式,组织包复制错误.

         初次之外,当使用移除`/var/lib/apt/lists`清除包缓存的时候,会降低镜像的大小.由于包缓存不会存在于镜像层中.执行更新指令的时候,包缓存总是会刷新的先与安装指令下包的版本.

      5.  管道的使用

         `RUN`指令可以依赖于管道输出.例如:

         ```shell
         RUN wget -O - https://some.site | wc -l > /number
         ```

         docker执行这些指令,通过的是`/bin/sh -c`编译器,这个仅仅计算最后一次操作的退出值,用于决定结果的成功或失败.

         如果你想产生失败加装`set -o pipefail` 确保执行一定会失败

         ```shell
         RUN set -o pipefail && wget -O - https://some.site | wc -l > /number
         ```
         
         注意不是所有shell 都支持`-o pipefail`指令
         
        6. [CMD指令](https://docs.docker.com/engine/reference/builder/#cmd)
      
           `CMD`指令需要使用在运行镜像中包含的软件,可以携带其他参数.使用形式为`CMD ["executable", "param1", "param2"…]` . 因此,对于一个服务镜像,比如Apache,你可以使用`CMD ["apache2","-DFOREGROUND"]` 运行.确实,这个指令推荐去执行基于镜像的服务.
      
           在多数状态下,`CMD`需要给定交互的shell指令.例如`CMD ["perl", "-de0"]`, `CMD ["python"]`或者`CMD ["php", "-a"]`.这个形式意味着你可以使用类似`docker run -it python`的指令.`CMD`很少联合`EntryPoint`执行`CMD ["param", "param"]`指令.
      
        7. [EXPOSE指令](https://docs.docker.com/engine/reference/builder/#expose)
      
           `EXPOSE`指令指明了容器中可以访问的端口,从结果上来说,你可以使用通用端口在你的应用上,例如,镜像中包含Apache web服务器,你可以使用`EXPOSE 80	`.使用`MongoDB`可以`EXPOSE 27017` 等等.
      
           在外部,执行`docker run`去指定入伙去映射端口关系.对于容器链接,docker环境变量可以返回这个资源.例如,`MYSQL_PORT_3306_TCP`.
      
        8. [ENV指令](https://docs.docker.com/engine/reference/builder/#env)
      
           使用`ENV`更新环境变量,使得应用根据简单的运行.例如.`ENV PATH /usr/local/nginx/bin:$PATH` 可以保证`CMD ["nginx"]`工作.
      
           `ENV`指令用于提供需要的环境变量给需要容器化的服务也是很有效的.比如Postgres : `PGDATA`
      
           最后使用`ENV`可以设置版本信息,例如
      
           ```shell
           ENV PG_MAJOR 9.3
           ENV PG_VERSION 9.3.4
           RUN curl -SL http://example.com/postgres-$PG_VERSION.tar.xz | tar -xJC /usr/src/postgress && …
           ENV PATH /usr/local/postgres-$PG_MAJOR/bin:$PATH
           ```
      
           如果在项目中存在一个相似的变量,就会运行你使用该指令指定一个版本的环境变量信息.每个`ENV`会创建一个中间层,就像`RUN`指令类似.意味着,就算你解除了环境变量的设置,但是在之后的层至,仍旧持久化到这层,且这个值不能被清除.可以使用下面的例子来测试:
      
           ```shell
           FROM alpine
           ENV ADMIN_USER="mark"
           RUN echo $ADMIN_USER > ./mark
           RUN unset ADMIN_USER
           ```
      
           ```shell
           $ docker run --rm test sh -c 'echo $ADMIN_USER'
           mark
           ```
      
           为了阻止这种情况的发送,已确保解除设置环境变量,使用`RUN`的shell指令去对环境变量进行单层设置.
      
           ```shell
           FROM alpine
           RUN export ADMIN_USER="mark" \
               && echo $ADMIN_USER > ./mark \
               && unset ADMIN_USER
           CMD sh
           ```
      
           再运行,查看结果
      
           ```shell
           $ docker run --rm test sh -c 'echo $ADMIN_USER'
           ```
      
        9. [ADD指令](https://docs.docker.com/engine/reference/builder/#add)
      
        10. [COPY指令](https://docs.docker.com/engine/reference/builder/#copy)
      
            `ADD`和`COPY`基本相似,总体来说,建议使用`COPY`.但是`COPY`指令只支持本地文件拷贝到容器中,而`ADD`有一些特征(本地tar包压缩或者远端URL支持).从结果上来说,`ADD`可以将本地tar包添加到镜像中.`ADD rootfs.tar.xz / .`
      
            如果有多个`dockerfile`步骤,使用了环境中不同的文件,单独的`COPY`它们.而非一次全部拷贝完毕.保证步骤的构建缓存时无效的.例如:
      
            ```shell
            COPY requirements.txt /tmp/
            RUN pip install --requirement /tmp/requirements.txt
            COPY . /tmp/
            ```
      
            对于RUN指令,导致了较少的缓存失效.比起你在前面使用COPY指令.
      
            由于进行大小的影响,使用`ADD`去获取来自远端URL的1包,推荐使用这个方法.使用`curl`或者`wget`指令.这种方式会删除你不再需要的文件.不需要将其他层添加到镜像,可以这样做:
      
            ```shell
            # 使用远端URL 添加,去除镜像大小的影响
            ADD http://example.com/big.tar.xz /usr/src/things/
            RUN tar -xJf /usr/src/things/big.tar.xz -C /usr/src/things
            RUN make -C /usr/src/things all
            ```
      
            也可以使用`curl`指令
      
            ```shell
            RUN mkdir -p /usr/src/things \
                && curl -SL http://example.com/big.tar.xz \
                | tar -xJC /usr/src/things \
                && make -C /usr/src/things all
            ```
      
        11. [ENTRYPOINT指令](https://docs.docker.com/engine/reference/builder/#entrypoint)
      
            `ENTRYPOINT`设置主命令的最优指令,使得镜像运行向它就是这个命令.
      
            使用`s3cmd`做示例演示:
      
            ```shell
            ENTRYPOINT ["s3cmd"] # 使用s3cmd工具 
            CMD ["--help"] #执行帮助指令
            ```
      
            镜像可以这样运行,就会显示指令的帮助信息
      
            ```shell
            docker run s3cmd
            ```
      
            使用正确的参数去执行指令
      
            ```shell
            docker run s3cmd ls s3://mybucket
            ```
      
            这个指令也可以用于合并辅助脚本,允许在开始工具需要多个步骤的时候,执行类似上述的步骤,例如Postgre:
      
            ```shell
            #!/bin/bash
            set -e
            if [ "$1" = 'postgres' ]; then
                chown -R postgres "$PGDATA"
                if [ -z "$(ls -A "$PGDATA")" ]; then
                    gosu postgres initdb
                fi
                exec gosu postgres "$@"
            fi
            exec "$@"
            ```
      
            将㕚脚本拷贝到容器，运行`ENTRYPOINT`指令
      
            ```dockerfile
            COPY ./docker-entrypoint.sh /
            ENTRYPOINT ["/docker-entrypoint.sh"]
            CMD ["postgres"]
            ```
      
            开启postgre
      
            ```shell
            docker run postgres
            ```
      
            或者带参数运行
      
            ```shell
            docker run postgres postgres --help
            ```
      
            最后也可以通过其他工具运行
      
            ```shell
            docker run --rm -it postgres bash
            ```
      
        12. [VOLUME指令](https://docs.docker.com/engine/reference/builder/#volume)
      
            `VOLUME`指令需要用于暴露数据块存储其余,配置存储或者docker容器创建的文件或者文件夹.建议使用`VOLUME`指令对于可变的或者用户服务的部分.
      
        13. [USER指令](https://docs.docker.com/engine/reference/builder/#user)
      
            如果服务没有执行权限,可以通过`USER`切换环岛一个非ROOT的用于,,可以创建用户或者用户组,示例如下:
      
            ```shell
            RUN groupadd -r postgres && useradd --no-log-init -r -g postgres postgres.
            ```
      
            请避免使用sudo,应为会造成不可预测的TTY,比如说启动服务需要root权限,但是运行时使用的是非root用户,考虑使用gosu.
      
            最后,为了降低层次和复杂度,请避免频繁切换`USER`
      
        14. [WORKDIR指令](https://docs.docker.com/engine/reference/builder/#workdir)
      
            为了提高可靠性,使用这个指令时尽量使用绝对地址.或者你也可以使用`WORKDIR`从而不去使用`RUN cd ... && do sth`这样的指令,可读性第,且会引起问题.
      
        15. [ONBUILD指令](https://docs.docker.com/engine/reference/builder/#onbuild)
      
            在`ONBUILD`指令执行完成之后,这个治好了从`FROM`当前镜像执行子镜像.由于`ONBUILD`指令在任何子`dockerfile`之前。`ONBUILD`指令对于构建`FROM`给定镜像构建镜像时十分有用的.例如,你可以使用`ONBUILD`用于追踪镜像.
      
            请注意`ONBUILD`中的ADD和COPY指令,如果新建的上下文环境中确实需要添加的资源,镜像会出现毁灭性的失败.推荐使用不同的标签,帮助迁移.

2. 创建基本镜像

   大多数dockerfile来源于一个父镜像,如果你需要完全控制镜像内容,需要创建基础镜像.

   +  `FROM`指令引入父镜像
   +  基础镜像dockerfile中含有`FROM scratch`

   #### 使用tar创建完全镜像

   总而言之,开始运行,就是运行你需要打包的父镜像.尽管在一些系统中不需要这样的操作.,下面构建一个简单的Ubuntu镜像:

   ```shell
   $ sudo debootstrap xenial xenial > /dev/null
   $ sudo tar -C xenial -c . | docker import - xenial
   
   a29c15f1bf7a
   
   $ docker run xenial cat /etc/lsb-release
   
   DISTRIB_ID=Ubuntu
   DISTRIB_RELEASE=16.04
   DISTRIB_CODENAME=xenial
   DISTRIB_DESCRIPTION="Ubuntu 16.04 LTS"
   ```

   当然也可以从Docker Github仓库中找到构建其他镜像的脚本

   #### 使用scratch创建简单父镜像

   当`scratch`出现在docker的库中时,你可以拉取它,运行它,使用scratch标签它.此外,你可以在自己的`dockerfile`中应用,下面就是创建最小镜像的示例:

   ```shell
   FROM scratch
   ADD hello /
   CMD ["/hello"]
   ```

   假设你建立一个[hello](https://github.com/docker-library/hello-world/)的示例,使用`-static`标记镜像编译.可以使用`docker build`进行构建.

   ```shell
   docker build --tag hello .
   ```

   最后的`.`时将构建上下文定位到当前目录的意思.

   这样就可以运行指令

   ```shell
   docker run --rm hello
   ```

3. 多段构建

   多段构建时Docker 17.05 之后的功能,当需要高效构建或者保证dockerfile易维护或者可读取强的必须工作.

   #### 构建前的准备

   最重要的是保证镜像大小尽可能小,每个指令都会添加镜像的层数.在移动到下一个层次之前,你需要清理掉你不需要的层次.为了高效的写dockerfile,可以使用传统的shell技巧使得层次能够尽可能的来源于上个层次.

   构建一个镜像,只包含应用和实际需要的依赖在开发环境下是很普遍的.

   这里是一个`Dockerfile.build`和`Dockerfile`,都是按照上述构成形式.

   `Dockerfile.build`

   ```dockerfile
   FROM golang:1.7.3
   WORKDIR /go/src/github.com/alexellis/href-counter/
   COPY app.go .
   RUN go get -d -v golang.org/x/net/html \
     && CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .
   ```

   注意到案例中两个RUN指令使用&& 合并起来,从而降低了层次的数量.

   `Dockerfile`

   ```dockerfile
   FROM alpine:latest  
   RUN apk --no-cache add ca-certificates
   WORKDIR /root/
   COPY app .
   CMD ["./app"]  
   ```

   `build.sh`

   ```shell
   #!/bin/sh
   echo Building alexellis2/href-counter:build
   
   docker build --build-arg https_proxy=$https_proxy --build-arg http_proxy=$http_proxy \  
       -t alexellis2/href-counter:build . -f Dockerfile.build
   
   docker container create --name extract alexellis2/href-counter:build  
   docker container cp extract:/go/src/github.com/alexellis/href-counter/app ./app  
   docker container rm -f extract
   
   echo Building alexellis2/href-counter:latest
   
   docker build --no-cache -t alexellis2/href-counter:latest .
   rm ./app
   ```

   当你运行这个脚本使,需要构建第一个镜像,创建第一个需要拷贝的容器.然后才能构建第二个镜像.每个镜像都会占用系统资源.

   #### 使用多段构建

   多段构建中,你可以使用多个FROM指令,每个FROM指令都可以使用一个不同的基础镜像.且每一个都会开始一个新的分段.你可以拷贝分段到另一个当中.你不需要的放在最终镜像中,下面是一个多段镜像的例子:

   `Dockerfile`

   ```dockerfile
   FROM golang:1.7.3
   WORKDIR /go/src/github.com/alexellis/href-counter/
   RUN go get -d -v golang.org/x/net/html  
   COPY app.go .
   RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .
   
   FROM alpine:latest  
   RUN apk --no-cache add ca-certificates
   WORKDIR /root/
   COPY --from=0 /go/src/github.com/alexellis/href-counter/app .
   CMD ["./app"]  
   ```

   这样就不需要分多次构建了,运行构建指令

   ```shell
   docker build -t alexellis2/href-counter:latest .
   ```

   最终结果是相同的,但是构建负载的降低.不需要创建中间镜像.你不需要分发安装包到本地文件系统上.

   如何运行的:

   第二个FROM指令开始一个新的分段,使用`alpine:lastest`作为基础镜像.`COPY --from=0`仅仅拷贝上一个分段的安装包到新的分段中.其他安装包被留下来,不会保存到最终镜像中.

   #### 命名构建分段(stage)

   默认情况下,分段没有命名,通过整数执行它们.首个FROM的分段命名为0.但是你也可通过`AS <NAME>`到`FROM`指令.这个实例通过命名分段和拷贝指令提前了前一个分段.这就一位置,就算你的docker文件重新排序了,这个CPOY还是不可以破坏的.

   ```dockerfile
   FROM golang:1.7.3 AS builder
   WORKDIR /go/src/github.com/alexellis/href-counter/
   RUN go get -d -v golang.org/x/net/html  
   COPY app.go    .
   RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .
   
   FROM alpine:latest  
   RUN apk --no-cache add ca-certificates
   WORKDIR /root/
   COPY --from=builder /go/src/github.com/alexellis/href-counter/app .
   CMD ["./app"]  
   ```

   #### 指定分段停止

   当不需要构建整个dockerfile的时候,你可以指定分段停止.命令如下(使用`builder`)

   ```shell
   docker build --target builder -t alexellis2/href-counter:latest .
   ```

   这种方式在下列情况下比较好使:

   1.  debug 指定构建stage
   2.  使用`debug`分段
   3.  在应用中属于测试分段,但是在生成环境下使用另一个stage(使用真实数据)

   #### 使用外部镜像存储

   使用多段构建时,对于拷贝之间docker文件的分段的拷贝是没有限制的.你可以使用`COPY -from`

   拷贝指定镜像.或者使用本地镜像名称,本地可识别标签或者docker注册器,或者标签ID.如果必须的话docker客户端需要拉取镜像,且从中拷贝.语法如下:

   ```shell
   COPY --from=nginx:latest /etc/nginx/nginx.conf /nginx.conf
   ```

   #### 以前一个分段作为起始分段

   可以在上一个分段的基础上构建新的分段

   ```dockerfile
   FROM alpine:latest as builder
   RUN apk --no-cache add build-base
   
   FROM builder as build1
   COPY source1.cpp source.cpp
   RUN g++ -o /binary source.cpp
   
   FROM builder as build2
   COPY source2.cpp source.cpp
   RUN g++ -o /binary source.cpp
   ```

4. 管理镜像

   #### Docker注册器

   docker注册器是docker生态系统的组件.注册器是一个存储器且内容分发系统,容纳docker镜像.可以使用在不同的标签版本中

   例如`distribution/registry`镜像,标签为`2.0`和`lastet`,通过push和pull指令与注册器进行交互:

   ```shell
   docker pull myregistry.com/stevvooe/batman:voice.
   ```

   **信任的docker注册器**

   是Docker版本编辑器的一部分,是私有的,完全的docker注册器.包含如下特征,**镜像标记**,**内容信任**,**基于身份获取权限**,且其他版本级别的特征.

   **内容信任**

   在网络系统间传送数据的时候,信任是集中考虑的.特殊情况下,当与一个不受信任的中间件沟通时.确保系统操作的数据的发布者和完整性是非常重要的.内容信任机制使得可以鉴别数据的完整性和数据的发布者.

5. Docker构建的提升