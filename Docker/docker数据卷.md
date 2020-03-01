### **数据卷**

---

#### 存储概述

默认情况下，所有文件在容器内部创建，存储在可写容器层。意味着

+  容器不存在的情况下，数据不会持久化，很难从容器外部获取数据。
+  容器可写层在容器运行时紧紧地与主机绑定。这是不能轻易地移动数据。
+  写到可写层，需要一个[存储驱动器](https://docs.docker.com/storage/storagedriver/)管理文件系统。存储驱动器提供一个联合的文件系统，使用的是Linux内核。额外的抽象降低了写到主机文件系统与数据卷的比较。

docker存在有两项配置，用于存储文件到主机上。所以文件在容器停止之后还是可以持久化的。分别是**volumes**，和**bind mounts**。如果在linux上运行docker，可以使用**tmpfs mount**配置。在windows上运行docker你可以使用命名的pipe。

下面接受各种类型数据的持久化方式。

#### 数据卷

数据卷对于容器产生的持久化数据是最优选择。**绑定挂载**依赖于主机的目录结构(文件系统)，而数据卷完全受到docker管理。数据卷有如下几个优点。

+  较**绑定挂载**更加易于备份和迁移。
+  使用docker命令行或者docker API管理数据卷。
+  数据卷可以在linux和windows平台上使用。
+  可以在多个容器之间共享。
+  数据卷可以允许你在远端主机和云端上存储，对数据卷内容进行加密，或者其他操作。
+  新数据卷可以基于之前的数据卷内容创建。

此外，数据卷通常情况下优于持久化数据到容器的可写层，因为数据卷不会增大容器的大小，且数据卷可以存活在给定容器的生命周期范围之外。

<img src="E:\截图文件\几种持久化方式的对比.png" style="zoom:67%;" />

如果容器产生了非持久化的状态数据，考虑到使用 [tmpfs mount](https://docs.docker.com/storage/tmpfs/) ，避免到处存储永久数据，且通过避免写入到容器的可写层，从而达到提升容器性能的效果。

1.  选择`-v`或者`--mount`参数

   这两个参数都是用于独立容器的,且`--mount`是用于swarm服务的.但是自从docker 17.06开始,可以使用`--mount`用于独立容器.总而言之,`--mount`更加明确可见,最大不同之处在于`-v`合并所有配置到一个属性中,而`--mount`将其进行分裂,这里有不同参数之间的对比.新手推荐使用`--mount`标签.

   > 如果你需要指定数据卷配置参数,必须使用`--mount`标签
   >
   > +  `-v`和`--volume`使用`:`将参数进行分割,属性顺序必须要正确,且属性意义要明确.
   >   +  已命名的数据卷中,第一个属性为数据卷的名称,在主机上具有唯一性.对于匿名的数据卷,首个参数省略
   >   +  第二个参数是文件/目录在容器中的挂载位置
   >   +  第三个参数是可选的,是以逗号分隔的配置集,
   > +  `--mount`包含多个kv对,使用逗号分隔,每个包含有`<key>=<value>`的形式,这个要比上面的表示的要明确,key的顺序不重要,值的标签也易于识别.
   >   +  挂载类型`type`,可以为`bind`,`tmpfs`,`volume`类型
   >   +  挂载的资源`source`,对于已经命名的数据卷,名称就是数据卷的名称.匿名数据卷名称省略,可以通过`source`或者`src`指定.
   >   +  使用`destination`指定容器中的挂载路径
   >   +  `readonly`属性,如果存在会使用只读的方式绑定到容器中
   >   +  `volume-option`属性,可以指定超过一次,采样kv对的配置方式

   	>如果数据卷驱动器接受了一个逗号分隔的配置,必须要去除掉外部csv转换器的值.需要去除的参数使用双引号`"`包围,整个参数部分使用单引号`'`包围.
   	>
   	>例如,本地驱动器接受了一个参数列表,下述例子正确的处理去除参数的配置
   	>
   	>```shell
   	>$ docker service create \
   	>     --mount 'type=volume,src=<VOLUME-NAME>,dst=<CONTAINER-PATH>,volume-driver=local,volume-opt=type=nfs,volume-opt=device=<nfs-server>:<nfs-path>,"volume-opt=o=addr=<nfs-address>,vers=4,soft,timeo=180,bg,tcp,rw"'
   	>    --name myservice \
   	>    <IMAGE>
   	>```

   支持使用`--mount`标签

   2.  创建和管理数据卷

      与绑定挂载不同,这个可以在容器之外创建和管理数据卷

      + 创建数据卷

        ```shell
        $ docker volume create my-vol
        ```

      +  显示数据卷

        ```shell
        $ docker volume ls
        local               my-vol
        ```

      +  检查数据卷

        ```shell
        $ docker volume inspect my-vol
        [
            {
                "Driver": "local",
                "Labels": {},
                "Mountpoint": "/var/lib/docker/volumes/my-vol/_data",
                "Name": "my-vol",
                "Options": {},
                "Scope": "local"
            }
        ]
        ```

      +  移除数据卷

        ```shell
        $ docker volume rm my-vol
        ```

   3.  使用数据卷启动容器

      如果使用不存在的数据卷启动容器,docker可以为你创建数据卷.下述示例将`/myvol2`挂载到`/app/`的容器下.`-v`和`--mount`效果相同.只有在运行`devtest`和`myvol2`移除之后,才可以同时使用这两个.

      +  `--mount`版本

        ```shell
        $ docker run -d \
          --name devtest \
          --mount source=myvol2,target=/app \
          nginx:latest
        ```

      +  `-v`版本

        ```shell
        $ docker run -d \
          --name devtest \
          -v myvol2:/app \
          nginx:latest
        ```

      +  检查数据卷是否成功创建

        ```shell
        "Mounts": [
            {
                "Type": "volume",
                "Name": "myvol2",
                "Source": "/var/lib/docker/volumes/myvol2/_data",
                "Destination": "/app",
                "Driver": "local",
                "Mode": "",
                "RW": true,
                "Propagation": ""
            }
        ],
        ```

        这里显示了正确的名称信息(source)和挂载信息(destination).这个挂载时读写权限.停止容器,并移除数据卷,注意到数据卷的移除是分步的.

        ```shell
        $ docker container stop devtest # 先停止容器
        
        $ docker container rm devtest # 删除容器
        
        $ docker volume rm myvol2 # 移除数据卷
        ```

   4.  使用数据卷启动服务

      当你启动服务，并定义数据卷的时候，每个服务容器使用自己的数据卷。如果你室友本地数据卷驱动器，没有容器可以共享数据，但是一些数据卷驱动器可以支持共享存储。AWS和AZure都支持使用`Cloudstor`插件的持久化存储.下述示例使用量`nginx`服务,使用了4个副本.每个都使用了叫做`myvol2`的本地数据卷.

      ```shell
      $ docker service create -d \
        --replicas=4 \
        --name devtest-service \
        --mount source=myvol2,target=/app \
        nginx:latest
      ```

      使用`docker service ps devtest-service`检查运行状况

      ```shell
      $ docker service ps devtest-service
      
      ID                  NAME                IMAGE               NODE                DESIRED STATE       CURRENT STATE            ERROR               PORTS
      4d7oz1j85wwn        devtest-service.1   nginx:latest        moby                Running             Running 14 seconds ago
      ```

      移除服务时,会停止所有任务

      ```shell
      $ docker service rm devtest-service
      ```

      这里仅仅移除了服务,没有移除服务创建的数据卷.

      +  语法层面的不同的

        `docker service create`不支持`-v`和`-volume`标签,必须要使用`--mount`标签.

   5.  使用容器构建数据卷

      如果你启动了一个创建数据卷的容器,容器有文件或者目录需要去挂载.目录的内容需要拷贝到数据卷中.然后容器挂载,并使用数据卷.其他容器就可以在数据卷的基础上进行扩展.

      举例说明,启动了一个`nginx`容器,并构建了一个`nginx-vol`数据卷,挂载在`/usr/share/nginx/html`目录下,这里是nginx存储默认html内容的位置.

      这里`--mount`和`-v`是一个执行效果

      + `--mount`执行版本

        ```shell
        $ docker run -d \
          --name=nginxtest \
          --mount source=nginx-vol,destination=/usr/share/nginx/html \
          nginx:latest
        ```

      + `-v`版本

        ```shell
        $ docker run -d \
          --name=nginxtest \
          -v nginx-vol:/usr/share/nginx/html \
          nginx:latest
        ```

      + 移除数据卷

        ```shell
        $ docker container stop nginxtest
        
        $ docker container rm nginxtest
        
        $ docker volume rm nginx-vol
        ```

   6.  使用只读型数据卷

      在一些开发应用中,容器需要写出到绑定挂载中以便于改变会传回到docker主机.其他情况下,容器仅仅需要读取数据即可.记住多个容器可以挂载同一个数据卷,可以有部分为读写权限,一些为只读权限.

      下述示例,修改了数据卷,但是挂载目录是只读形式,通过添加`ro`到配置列表中.如果传入多个参数,使用逗号分隔.

      使用`--mount`和`-v`效果相同

      + 使用`--mount`

        ```shell
        $ docker run -d \
          --name=nginxtest \
          --mount source=nginx-vol,destination=/usr/share/nginx/html,readonly \
          nginx:latest
        ```

      + 使用`-v`

        ```shell
        $ docker run -d \
          --name=nginxtest \
          -v nginx-vol:/usr/share/nginx/html:ro \
          nginx:latest
        ```

      + 使用`docker inspect nginxtest`检查数据卷挂载信息

        ```shell
        "Mounts": [
            {
                "Type": "volume",
                "Name": "nginx-vol",
                "Source": "/var/lib/docker/volumes/nginx-vol/_data",
                "Destination": "/usr/share/nginx/html",
                "Driver": "local",
                "Mode": "",
                "RW": false,
                "Propagation": ""
            }
        ],
        ```

      +  移除数据卷

        ```shell
        $ docker container stop nginxtest
        
        $ docker container rm nginxtest
        
        $ docker volume rm nginx-vol
        ```

   7.  多机共享数据

   8.  使用数据卷驱动器

   9.  数据卷数据的备份,恢复和迁移

   10.  移除数据卷

#### 绑定挂载

#### 临时文件系统挂载

#### 排除故障数据卷

#### 容器内存储数据