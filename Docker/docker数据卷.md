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

   7. 多机共享数据

      当对一个应用进行容错的时候,需要设置多个服务副本,这些服务都可以共享数据文件.

      <img src="E:\截图文件\副本共享机制.png" style="zoom:67%;" />

      当部署应用时,有很多部署方法,一种方法是添加逻辑到应用中,用户存储文件到云存储器中.这种的典型示例就是S3.另一种是使用驱动器创建数据集,这种支持写文件到外部存储系统中,比如NFS或者S3.
   
      数据集驱动器允许你在应用逻辑的层面对底层存储系统的抽象.例如,如果你是要NFS驱动器的数据卷.可以使用不同的驱动器作为云存储的示例数据更新服务.这样就不需要改变应用逻辑了.
   
   8. 使用数据卷驱动器
   
      当使用`docker volume create`创建数据卷的时候,如果你使用还没有创建的数据卷启动容器时,你可以指定一个驱动器,下述示例使用的是`vieux/sshfs`作为驱动器.首次创建独立数据卷.然后启动创建新数据卷的容器.
   
      +  初始化构建
   
        下个示例假设你有两个节点,第一个节点的docker主机使用SSH连接到第二个节点.在docker主机上,使用`vieux/sshfs`插件:
   
        ```shell
        $ docker plugin install --grant-all-permissions vieux/sshfs
        ```
   
      + 使用数据卷驱动器创建数据卷
   
        这个示例指定了ssh密码,如果两台主机共享同一个密码.可以抛弃密码的设定,每个驱动器可以包含零或者多个配置项.使用`-o`标记参数设定
   
        ```shell
        $ docker volume create --driver vieux/sshfs \
          -o sshcmd=test@node2:/home/test \
          -o password=testpassword \
          sshvolume
        ```
   
      + 启动创建数据卷的容器
   
        这个示例指定了ssh密码,如果两台主机共享同一个密码.可以抛弃密码的设定,每个驱动器可以包含零或者多个配置项.如果数据卷驱动器需要你传递参数,必须使用`--mount`标签,不能使用`-v`标签.
   
        ```shell
        $ docker run -d \
          --name sshfs-container \
          --volume-driver vieux/sshfs \
          --mount src=sshvolume,target=/app,volume-opt=sshcmd=test@node2:/home/test,volume-opt=password=testpassword \
          nginx:latest
        ```
   
      + 创建服务(创建NFS数据卷)
   
        This example shows how you can create an NFS volume when creating a service. This example uses `10.0.0.10` as the NFS server and `/var/docker-nfs` as the exported directory on the NFS server. Note that the volume driver specified is `local`.
   
        创建服务的时候,这个示例提供了创建NFS数据卷的示例.这个例子使用了`10.0.0.10`作为NFS服务器.使用`/var/docker-nfs`作为服务器导出目录.注意到数据卷驱动器指定为`local`.
   
        ```shell
        # NFSV3
        $ docker service create -d \
          --name nfs-service \
          --mount 'type=volume,source=nfsvolume,target=/app,volume-driver=local,volume-opt=type=nfs,volume-opt=device=:/var/docker-nfs,volume-opt=o=addr=10.0.0.10' \
          nginx:latest
          
        # NFSV4
        docker service create -d \
            --name nfs-service \
            --mount 'type=volume,source=nfsvolume,target=/app,volume-driver=local,volume-opt=type=nfs,volume-opt=device=:/,"volume-opt=o=10.0.0.10,rw,nfsvers=4,async"' \
            nginx:latest
        ```
   
   9. 数据卷数据的备份,恢复和迁移
   
      数据卷易于备份,恢复和迁移.使用`--volume-from`标签创建挂在了数据卷的容器.
   
      + 备份容器
   
        创建一个叫做`dbstore`的容器
   
        ```shell
        $ docker run -v /dbstore --name dbstore ubuntu /bin/bash
        ```
   
        下面的指令做了如下几个动作
   
        1.  允许容器,并将数据卷挂在到容器中
        2. 挂在本地目录`backup`
        3.  传递压缩指令,压缩为`backup`文件,放入`/backup`中.
   
        ```shell
        $ docker run --rm --volumes-from dbstore -v $(pwd):/backup ubuntu tar cvf /backup/backup.tar /dbdata
        ```
   
        指令完成但是容器停止的时候,可以找到`dbstore`数据卷的备份文件
   
      + 从备份恢复容器
   
        创建完备份之后,可以恢复到相同的容器.获取指定的其他容器.例如可以指定一个`dbstore2`容器
   
        ```shell
        $ docker run -v /dbdata --name dbstore2 ubuntu /bin/bash
        ```
   
        使用解压指令展开数据卷文件
   
        ```shell
        $ docker run --rm --volumes-from dbstore2 -v $(pwd):/backup ubuntu bash -c "cd /dbdata && tar xvf /backup/backup.tar --strip 1"
        ```
   
   10. 移除数据卷
   
       容器删除之后,docker数据卷会持久化.数据卷需要考虑到两点:
   
       + 匿名数据卷
   
         容器删除时,没有指定的资源去指导docker引擎去移除他们
   
         使用`-rm`参数移除匿名数据卷,下述指令删除的是`/foo`数据卷
   
         ```shell
         $ docker run --rm -v /foo -v awesome:/bar busybox top
         ```
   
       + 命名数据卷
   
         外部有指定的资源,例如`awesome:/bar`
   
         移除所有的数据卷(未使用),并释放内存空间
   
         ```shell
         $ docker volume prune
         ```

#### 绑定挂载

绑定挂载在docker使用之初就存在了,绑定挂在在数据卷之间的比较之间有着功能限制.当你使用绑定挂载的时候,主机文件/目录会被挂在到容器中.使用绝对/相对定位引用到主机地址.相反地,当你使用数据卷的时候,新的目录创建在docker存储目录下,docker管理这目录的内容.

文件或者目录不需要存在于docker主机上.根据要求创建.绑定挂载性能优越,但是需要主机文件系统指定文件可用目录.如果你正在开发新的docker文件,考虑使用**命名数据卷**的方式.不能使用docker命令行去之间管理绑定端口.

1.  `-v`和`--mount`标签的使用

   开始的时候`-v`或者`--volume`用于独立运行的容器,而`--mount`用于swarm服务.但是17.06之后,也可以使用`--mount`在独立容器中了.由于`-mount`良好的可见性,推荐使用.

   >+ `-v`或者`--volume` : 包含三个属性,属性之间使用`:`隔开,属性必须符合顺序要求
   >  1. 第一个属性: 主机名称
   >  2. 第二个属性: 容器挂载位置
   >  3. 使用逗号隔开的配置项
   >+ `--mount`标签: 包含多个kv对,按照`<key>=<value>`的形式给出,有几个特殊指定参数如下:
   >  1. 类型`type`: 可以选择`bind`,`volume`或者`tmpfs`三种类型
   >  2. 主机位置`source`: 对于绑定挂载来说,路径就是docker主机的位置路径.使用`source`或者`src`指定
   >  3. 挂载目录`destination`: 挂载位置,使用`destination`,`dst`或者`target`指定.
   >  4. 只读`readonly`: 存在的情况下,这次挂载是只读的.
   >  5. 绑定权限`bind-propagation`: 可以选择`rprivate`,`private`,`rshared`,`shared`,`rslave`或者`slave`.
   >  6. 一致性`consistency`,可以配置为`consistent`,`delegated`或者`cached`只有Mac平台能够配置
   >  7. `--mount`标记在selinux模式下不支持`z`或者`Z`属性配置.

+ `-v`和`--mount`标签的区别

  如果你使用`-v`标签绑定docker主机上不存在的文件/目录,`-v`会创建一个后台.且总是会去生成目录.

  如果你是要`--mount`标签绑定docker主机上不存在的文件/目录,`--mount`不会自动给你创建后台,且会引发错误.

2. 绑定挂载启动

   考虑到已经创建了`source`目录,构建出的内容会保存到`source/target`目录下,如果需要自定义绑定可以通过下述指令完成.注意,`&pwd`子指令扩展了当前工作目录到linux和MacOS上.

   ```shell
# 使用mount
   $ docker run -d \
  -it \
     --name devtest \
     --mount type=bind,source="$(pwd)"/target,target=/app \ # 指令了挂载目标位置为/app
     nginx:latest
   # 使用v
   $ docker run -d \
     -it \
     --name devtest \
     -v "$(pwd)"/target:/app \
     nginx:latest
   ```
   
   检查挂载位置是否执行成功
   
   ```shell
"Mounts": [
       {
        "Type": "bind",
           "Source": "/tmp/source/target",
           "Destination": "/app",
           "Mode": "",
           "RW": true,
           "Propagation": "rprivate"
       }
   ],
   ```
   
   关闭容器
   
   ```shell
$ docker container stop devtest
   $ docker container rm devtest
```
   
   + 挂载到容器中的非空目录
   
     如果你挂载到一个非空目录中,目录中存在有绑定加载覆盖的内容部分.使用这个是比较有利的.下述示例使用`tmp`目录覆盖了容器中的`/usr`目录.在多数情况下,会使得容器功能丧失.

     ```shell
  # -v
     $ docker run -d \
    -it \
       --name broken-container \
       -v /tmp:/usr \
       nginx:latest
     
     docker: Error response from daemon: oci runtime error: container_linux.go:262:
     starting container process caused "exec: \"nginx\": executable file not found in $PATH".
     
     # --mount
     $ docker run -d \
       -it \
       --name broken-container \
       --mount type=bind,source=/tmp,target=/usr \
       nginx:latest
     
     docker: Error response from daemon: oci runtime error: container_linux.go:262:
     starting container process caused "exec: \"nginx\": executable file not found in $PATH".
     
     ```
   
     容器此时创建完成,但是没有启动,直接移除即可
   
     ```shell
  $ docker container rm broken-container
     ```

3. 使用只读的绑定挂载

   对于一些开发应用中,容器需要写入到绑定加载中,所以改变会传递会docker主机上.与此同时,容器仅仅需要读权限.下述示例修改一个但是绑定目录作为一个只读的绑定加载,通过添加`ro`到列表中.如果需要传入多个参数,使用逗号进行分割.

   ```shell
   # --mount
   $ docker run -d \
     -it \
     --name devtest \
     --mount type=bind,source="$(pwd)"/target,target=/app,readonly \
     nginx:latest
   # -v
   $ docker run -d \
     -it \
     --name devtest \
     -v "$(pwd)"/target:/app:ro \
     nginx:latest
   ```

   检查创建的容器

   ```shell
   "Mounts": [
       {
           "Type": "bind",
           "Source": "/tmp/source/target",
           "Destination": "/app",
           "Mode": "ro",
           "RW": false,
           "Propagation": "rprivate"
       }
   ],
   ```

   停止容器

   ```shell
   $ docker container stop devtest
   $ docker container rm devtest
   ```

4. 配置绑定属性

   在绑定加载和数据卷模式下,默认的绑定属性为`rprivate`.表示仅仅用于绑定配置,且只能用于linux上.绑定权限是个高级topic,大多数用户不需要使用.

   绑定传输属性指的是是否使用给定的绑定加载/命名数据卷会创建挂载的副本.考虑到挂载点`/mnt`,同时也会挂载到`tmp`上.当挂载到`/tmp/a`也会挂载到`/mnt/a`上,每次传输都有迭代计数.在迭代的情况下,`/tmp/a`作为`/foo`的挂载.

   | 传输属性   | 描述                                                         |
| ---------- | ------------------------------------------------------------ |
   | `shared`   | 原始挂载的子挂载作为副本挂载,副本挂载的子挂载传输给原始挂载. |
| `slave`    | 类似于共享挂载`shared`,但是仅仅在一个方向上.即如果原始挂载暴露了子挂载,那么副本是可以看到的.但是如果副本挂载保留了一个子挂载,原始挂载是找不到这个挂载的. |
   | `private`  | 挂载是私有的,子挂载不会暴露给副本挂载.副本挂载的子挂载不会保留给原始挂载 |
   | `rshared`  | 与`shared`类型类似.但是传输上进行了扩展,可以支持多副本挂载点与原始挂载的共享. |
   | `rslave`   | 与`slave`类似,扩展为可以看到多个挂载点                       |
   | `rprivate` | 默认情况,没有原始挂载点与副本挂载点直接是可以互相看到的.     |
   
   在你设置挂载点的绑定传输时,主机文件系统需要支持绑定创数.关于绑定传输可以参考:
   
   <(https://www.kernel.org/doc/Documentation/filesystems/sharedsubtree.txt>

   下述示例绑定了`target`目录到容器两次,第二次绑定设置了`ro`属性,和`rslave`传输属性

   ```shell
# -v
   $ docker run -d \
  -it \
     --name devtest \
  -v "$(pwd)"/target:/app \
     -v "$(pwd)"/target:/app2:ro,rslave \
  nginx:latest
   # --mount
$ docker run -d \
     -it \
     --name devtest \
     --mount type=bind,source="$(pwd)"/target,target=/app \
     --mount type=bind,source="$(pwd)"/target,target=/app2,readonly,bind-propagation=rslave \
     nginx:latest
   ```
   
5. 配置selinux标签

   使用`selinux`时,可以添加`z`或者`Z`属性,修改selinux主机挂载文件/目录标签

   + `z`表示挂载内容共享与多个容器
   + `Z`表示挂载内容时私有的且非公用的
   + 使用时绑定系统目录,`/home`或者`/usr`时`Z`时不可以使用的,必须手动对系统目录重新进行标签.

   下述是一个`z`的使用示例

   ```shell
   $ docker run -d \
     -it \
     --name devtest \
     -v "$(pwd)"/target:/app:z \
     nginx:latest
   ```

#### 临时文件系统挂载

1. 临时文件系统绑定的限制

   + 与数据卷和绑定挂载不同，不可以使用`tmpfs`进行容器之间的共享
   + 只能在linux操作系统下使用

2. `--tmpfs`和`--mount`标签的区别

   原始情况下,`--tmpfs`标记用于独立容器,且`--mount`用于swarm服务.Docker 17.06之后的版本,可以在度量容器中使用`--mount`标记.总体来说`--mount`参数具有较高的可识别性,且`--tmpfs`不支持可配置参数的配置

   + `--tmpfs`: 不允许去指定配置项,只能用于独立容器
   + `--mount`:包含多个kv对参数列表
     1.  `type`表示挂载的类型，可以选择`bind`,`volume`或者`tmpfs`三种类型
     2. `destination`指定挂载的位置,还可以使用`dst`,`target`指定
     3. `tmpfs-size`和`tmpfs-mode`配置

   优先使用`--mount`标签

3. 容器中使用`tmpfs`绑定

   使用`--tmpfs`或者`--mount`将`tmpfs`绑定到容器中.使用`tmpfs`绑定是不存在有`source`项的.下述示例创建了一个Nginx容器中的`/app`下的`tmpfs`挂载.

   ```shell
   # mount
   $ docker run -d \
     -it \
     --name tmptest \
     --mount type=tmpfs,destination=/app \
     nginx:latest
   # tmpfs
   $ docker run -d \
     -it \
     --name tmptest \
     --tmpfs /app \
     nginx:latest
   ```

   使用`docker container inspect tmptest`验证绑定情况

   ```shell
   "Tmpfs": {
       "/app": ""
   },
   ```

   移除容器

   ```shell
   $ docker container stop tmptest
   $ docker container rm tmptest
   ```

   + 指定的tmpfs参数

     `tmpfs` mounts allow for two configuration options, neither of which is required. If you need to specify these options, you must use the `--mount` flag, as the `--tmpfs` flag does not support them.

     `tmpfs`挂载允许两种配置属性,如果你需要指定参数,必须使用`--mount`标签,因为`--tmpfs`不支持.

     | 配置项       | 描述                                                      |
     | ------------ | --------------------------------------------------------- |
     | `tmpfs-size` | 挂载大小(字节数)                                          |
     | `tmpfs-mode` | 八进制文件读写木事,例如0770.默认情况下是1777,表示全局可写 |

     设置模式为`1770`,这个在容器内部不是全局可写的.

     ```shell
     docker run -d \
       -it \
       --name tmptest \
       --mount type=tmpfs,destination=/app,tmpfs-mode=1770 \
       nginx:latest
     ```

#### 排除故障数据卷

这里讨论溢写使用docker数据卷时常见的错误

`Error: Unable to remove filesystem`

一些基于容器的使用,比如`Google cAdvisot`,挂载到docker系统的目录上,假设这个目录为`/var/lib/docker`/.文档中建议你按照下述方法运行`cadvisor`容器

```shell
$ sudo docker run \
  --volume=/:/rootfs:ro \
  --volume=/var/run:/var/run:rw \
  --volume=/sys:/sys:ro \
  --volume=/var/lib/docker/:/var/lib/docker:ro \
  --publish=8080:8080 \
  --detach=true \
  --name=cadvisor \
  google/cadvisor:latest
```

当你绑定到这个目录时,高效绑定所有资源,作为容器的文件系统.如果你需要移除某个容器.请求会失败.

```shell
Error: Unable to remove filesystem for
74bef250361c7817bee19349c93139621b272bc8f654ae112dd4eb9652af9515:
remove /var/lib/docker/containers/74bef250361c7817bee19349c93139621b272bc8f654ae112dd4eb9652af9515/shm:
Device or resource busy
```

问题发生在当容器绑定到`/var/lib/docker/`上的时候,使用`statfs`或者`fstatfs`在文件系统上处理`/var/lib/docker`但是并没有关闭.

建议这种情况下不要使用绑定挂载,因为绑定挂载需要CPU支持.

如果你不确定进程是否出现上述情况,可以使用`lsof`命令找到这个进程.(这里使用的是上面挂载位置)

```shell
$ sudo lsof /var/lib/docker/containers/74bef250361c7817bee19349c93139621b272bc8f654ae112dd4eb9652af9515/shm
```

#### 容器内存储数据

1.  存储驱动器

   为了能够高效的使用存储驱动器,了解docker是如何构建,以及存储镜像,还有镜像如何使用容器的就比较重要了.可以使用这些信息使得你的应用具有最优的持久化数据选择.且避免一些性能问题.

   存储驱动器允许在容器可写层创建数据,数据持久化之后不会持久化,读写速度都比本地文件系统要慢.

2.  选择存储驱动器

3.  使用AUFS存储驱动器

4.  使用Btrfs存储驱动器

5.  使用设备映射存储驱动器

6.  使用Overlay存储驱动器

7.  使用ZFS存储驱动器

8.  使用VFS存储驱动器