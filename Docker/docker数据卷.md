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

1. 存储驱动器

   为了能够高效的使用存储驱动器,了解docker是如何构建,以及存储镜像,还有镜像如何使用容器的就比较重要了.可以使用这些信息使得你的应用具有最优的持久化数据选择.且避免一些性能问题.

   存储驱动器允许在容器可写层创建数据,数据持久化之后不会持久化,读写速度都比本地文件系统要慢.

   + 镜像和层

     docker镜像有多个层构建而成，每层代表镜像dockerfile的一个构建指令。每层都是只读的，参考下述dockerfile文件：

     ```dockerfile
     FROM ubuntu:18.04
     COPY . /app
     RUN make /app
     CMD python /app/app.py
     ```

     docker文件中有4个指令,每个指令创建一个层.分别是:

     1.  `FROM`由基础镜像`ubuntu:18.04`创建层
     2. `COPY`添加文件到docker客户端目录中
     3. `RUN`使用make指令构建应用
     4. `CMD`指定容器中运行的指令

     每层仅仅是与上一层的不同部分,层是堆叠在之前的层之上的.当你创建一个新的容器的时候,在底层上创建一个新的可写层.这个叫做**容器层**.所有对运行中容器的改变(写新文件,修改存在的文件,删除文件),都会写到这个薄的容器层中.如图所示:

     <img src="E:\截图文件\层的建立.png" style="zoom:67%;" />

2. 选择存储驱动器

   容器和镜像之间主要的不同是顶层的可写层.新增或者修改现存与可写层会写到容器中.但是底层的镜像是不会发生变化的.

   由于每个容器都有可写层,所有对容器可写层的改变都会存储到可写层中.多个容器可以共享同一个底层镜像,然而数据状态则是独立的.下图标示量Ubuntu 18.04镜像的共享示意图:

   <img src="E:\截图文件\镜像与容器的关系.png" style="zoom:67%;" />

   如果需要对个镜像共享相同的数据，将数据存储到docker的数据卷中，并将其挂载到容器中。

   docker使用存储驱动器，去管理镜像层和容器可写层的内容，每个存储驱动器处理的实现时不同的，但是所有驱动器使用堆叠的镜像层，且使用写时拷贝(CoW)策略.

   + 此外上的容器大小

     为了查看运行容器的合适大小,可以使用`docker ps -s`指令查看先关大小

     + `size`: 磁盘数据的数量,用于每个容器的可写层
     + `virtual size`: 用于只读镜像数据的数量(容器加上容器可写层的大小).多个容器可以共享多个只读镜像数据.两个容器开始于同样的镜像共享100%只读数据,两个不同降镜像的容器可以共享相同的可写层.因此,不能只计算合计的虚拟容器大小.

     磁盘上运行容器的总计磁盘空间时每个容器大小与虚拟存储大小之和.如果多个容器起始于一个确定的镜像.容器的磁盘总大小会变成容器实际大小+虚拟空间大小.

     下述方式也会占用磁盘空间：
     
     + 使用`json-file`对驱动器进行日志记录,那么磁盘上就需要有这些日志记录文件的空间.
     + 数据卷和容器绑定挂载
     + 容器配置文件,一般情况下比较小
   +  内存交换的内容(如果开启了内存交换)
   
+ 检查点(如果你使用检查点,用于恢复数据)
  
+ 写时拷贝策略(CoW)
  
  **写时拷贝**策略是为了达到高效的共享和复制文件.如果文件存在于镜像的底层,其他层(包括可写层)需要读取.他使用存在的文件.其他层首次需要修改文件时(构建和运行容器的时候),文件被拷贝到那一层并进行修改.使得每个结果层的IO最小化.
  
  + 共享更小的镜像
  
    当使用`docker pull`指令去从库中拉取镜像的时候,或者根据镜像创建不存在的容器的时候,每一层都会单独的被拉取下来,且存储在docker本地存储区域中,使用`/var/lib/docker`的目录上.下述以ubuntun作为示例:
  
       ```shell
       $ docker pull ubuntu:18.04
       18.04: Pulling from library/ubuntu
       f476d66f5408: Pull complete
       8882c27f669e: Pull complete
       d9af21273955: Pull complete
       f5029279ec12: Pull complete
       Digest: sha256:ab6cb8de3ad7bb33e2534677f865008535427390b117d7939193f8d1a6613e34
       Status: Downloaded newer image for ubuntu:18.04
       ```
  
    每层都存储在docker主机本地存储区域的目录中.为了检测文件系统的层,列举`/var/lib/docker/<storage-driver>`的内容,这个例子使用`overlay2`存储驱动器.
  
       ```shell
       $ ls /var/lib/docker/overlay2
       16802227a96c24dcbeab5b37821e2b67a9f921749cd9a2e386d5a6d5bc6fc6d3
       377d73dbb466e0bc7c9ee23166771b35ebdbe02ef17753d79fd3571d4ce659d7
       3f02d96212b03e3383160d31d7c6aeca750d2d8a1879965b89fe8146594c453d
       ec1ec45792908e90484f7e629330666e7eee599f08729c93890a7205a6ba35f5
       l
       ```
  
    The directory names do not correspond to the layer IDs (this has been true since Docker 1.10).
  
    Now imagine that you have two different Dockerfiles. You use the first one to create an image called `acme/my-base-image:1.0`.
  
    目录不会对层编号做出显示.有两个不同的dockerfile,第一个创建一个`acme/my-base-image:1.0`的镜像.
  
       ```dockerfile
       FROM ubuntu:18.04
       COPY . /app
       ```
  
    第二个镜像基于第一个镜像创建,但是还有其他额外的层
  
       ```dockerfile
       FROM acme/my-base-image:1.0
       CMD /app/hello.sh
       ```
  
    第二个镜像中包含第一个镜像的所有层,使用`CMD`指令添加新的层,且是一个读写权限的容器层.docker含有第一个镜像的所有层,所以不需要再拉取了.两个镜像共享相同的层.
  
    如果使用两个docker文件构建镜像,可以使用`docker image ls`和`docker history`指令确认加密ID和共享层是否一致.
  
    1.  创建`cow-test/`目录,并切换到目录中
  
    2. 在目录中,创建`hello.sh`文件
  
          ```shell
          #!/bin/sh
          echo "Hello world"
       ```
  
       修改执行权限
  
          ```shell
          chmod +x hello.sh
          ```
  
    3. 拷贝第一个dockerfile到一个新的文件`Dockerfile.base`中
  
    4. 拷贝第二个文件到一个新文件`Dockerfile`中
  
    5. 在`cow-test`目录下,构建第一个镜像,设置`PATH`,告知docker在何处添加镜像
  
          ```shell
          $ docker build -t acme/my-base-image:1.0 -f Dockerfile.base .
          Sending build context to Docker daemon  812.4MB
          Step 1/2 : FROM ubuntu:18.04
           ---> d131e0fa2585
          Step 2/2 : COPY . /app
           ---> Using cache
           ---> bd09118bcef6
          Successfully built bd09118bcef6
          Successfully tagged acme/my-base-image:1.0
       ```
  
    6. 构建第二个镜像
  
          ```shell
          $ docker build -t acme/my-final-image:1.0 -f Dockerfile .
          Sending build context to Docker daemon  4.096kB
          Step 1/2 : FROM acme/my-base-image:1.0
           ---> bd09118bcef6
          Step 2/2 : CMD /app/hello.sh
           ---> Running in a07b694759ba
           ---> dbf995fc07ff
          Removing intermediate container a07b694759ba
          Successfully built dbf995fc07ff
          Successfully tagged acme/my-final-image:1.0
       ```
  
    7.  检查镜像的大小
  
          ```shell
          $ docker image ls
          REPOSITORY                         TAG                     IMAGE ID            CREATED             SIZE
          acme/my-final-image                1.0                     dbf995fc07ff        58 seconds ago      103MB
          acme/my-base-image                 1.0                     bd09118bcef6  
          ```
  
    8. 检查每个镜像的层信息
  
          ```shell
          $ docker history bd09118bcef6
          IMAGE               CREATED             CREATED BY                                      SIZE                COMMENT
          bd09118bcef6        4 minutes ago       /bin/sh -c #(nop) COPY dir:35a7eb158c1504e...   100B                
          d131e0fa2585        3 months ago        /bin/sh -c #(nop)  CMD ["/bin/bash"]            0B                  
          <missing>           3 months ago        /bin/sh -c mkdir -p /run/systemd && echo '...   7B                  
          <missing>           3 months ago        /bin/sh -c sed -i 's/^#\s*\(deb.*universe\...   2.78kB              
          <missing>           3 months ago        /bin/sh -c rm -rf /var/lib/apt/lists/*          0B                  
          <missing>           3 months ago        /bin/sh -c set -xe   && echo '#!/bin/sh' >...   745B                
          <missing>           3 months ago        /bin/sh -c #(nop) ADD file:eef57983bd66e3a...   103MB      
       ```
  
          ```shell
          $ docker history dbf995fc07ff
          IMAGE               CREATED             CREATED BY                                      SIZE                COMMENT
          dbf995fc07ff        3 minutes ago       /bin/sh -c #(nop)  CMD ["/bin/sh" "-c" "/a...   0B                  
          bd09118bcef6        5 minutes ago       /bin/sh -c #(nop) COPY dir:35a7eb158c1504e...   100B                
          d131e0fa2585        3 months ago        /bin/sh -c #(nop)  CMD ["/bin/bash"]            0B                  
          <missing>           3 months ago        /bin/sh -c mkdir -p /run/systemd && echo '...   7B                  
          <missing>           3 months ago        /bin/sh -c sed -i 's/^#\s*\(deb.*universe\...   2.78kB              
          <missing>           3 months ago        /bin/sh -c rm -rf /var/lib/apt/lists/*          0B                  
          <missing>           3 months ago        /bin/sh -c set -xe   && echo '#!/bin/sh' >...   745B                
          <missing>           3 months ago        /bin/sh -c #(nop) ADD file:eef57983bd66e3a...   103MB  
          ```
  
       注意所有的层都被标识处理,处理第二个镜像的顶层.且其他层,两个进行共享,仅仅存储在`/var/lib/docker`中,新的层不占用任何空间,因为不改变任何文件,仅仅运行了一个指令.
  
          > 注意: `docker history`丢失的行内容会建立在其他文件系统上,且本地不可以获取.这个可以忽略

  + 高效拷贝构建的容器
  
    开启容器的时候,一个薄的可写容器层放置在其他层的顶部,所有对文件系统的修改防止在这之上,不会修改的文件不会复制到可写层上.这意味着可写层越小越好.
  
    当容器中存在的文件修改的时候,存储驱动器使用**cow策略**.指定步骤依赖于指定的存储驱动器.对于`aufs`,`overlay`和`overlay2`驱动器,cow策略遵循下述特征:
  
    - 寻找需要更新文件的镜像层,进程起始于最新的层,且基于基础层工作.当结果找到的时候,添加到缓存中,用于加速任务的操作.
    - 在第一个文件找到的时候进行`copy_up`操作,将文件拷贝到容器的可写层.
    - 对文件副本做的修改和容器不可以获取到到存在于底层中的只读副本.
  
    Brtfs,ZFS和其他驱动器处理cow的方式不同,可以在阅读相应的处理方法.
  
    写大量数据的容器需要占有更多的空间,因为写操作约到,消费顶层可写层的空间越大.
  
    > 注意: 对于重载应用,不能在容器内存储数据,相反,使用docker数据卷,这个独立于运行容器,设置于高效IO.紫外,数据卷在容器之间共享,且不会增加可写层的大小
  
  见的性能开销提升,这里的开销随着存储驱动器的不同而改变.具有多层的大文件和深层目录可能产生更深的提升.每个操作仅仅发生在文件第一次被修改的时候.
  
  为了证实cow工作方式,下述程序在`acme/my-final-image:1.0`上旋转获得5个容器。注意在Mac和windows平台上无法使用。
  
  1. 从docker主机的终端,运行`docker run`指令
  
  ```shell
  $ docker run -dit --name my_container_1 acme/my-final-image:1.0 bash \
    && docker run -dit --name my_container_2 acme/my-final-image:1.0 bash \
    && docker run -dit --name my_container_3 acme/my-final-image:1.0 bash \
    && docker run -dit --name my_container_4 acme/my-final-image:1.0 bash \
    && docker run -dit --name my_container_5 acme/my-final-image:1.0 bash
  
    c36785c423ec7e0422b2af7364a7ba4da6146cbba7981a0951fcc3fa0430c409
    dcad7101795e4206e637d9358a818e5c32e13b349e62b00bf05cd5a4343ea513
    1e7264576d78a3134fbaf7829bc24b1d96017cf2bc046b7cd8b08b5775c33d0c
    38fa94212a419a082e6a6b87a8e2ec4a44dd327d7069b85892a707e3fc818544
    1a174fc216cccf18ec7d4fe14e008e30130b11ede0f0f94a87982e310cf2e765
  ```
  
  2. 运行`docker ps`检查五个容器正在运行
  
     ```shell
     CONTAINER ID      IMAGE                     COMMAND     CREATED              STATUS              PORTS      NAMES
     1a174fc216cc      acme/my-final-image:1.0   "bash"      About a minute ago   Up About a minute              my_container_5
     38fa94212a41      acme/my-final-image:1.0   "bash"      About a minute ago   Up About a minute              my_container_4
     1e7264576d78      acme/my-final-image:1.0   "bash"      About a minute ago   Up About a minute              my_container_3
     dcad7101795e      acme/my-final-image:1.0   "bash"      About a minute ago   Up About a minute              my_container_2
     c36785c423ec      acme/my-final-image:1.0   "bash"      About a minute ago   Up About a minute              my_container_1
     ```
  
  3. 列举本地存储区域的内容
  
     ```shell
     $ sudo ls /var/lib/docker/containers
     
     1a174fc216cccf18ec7d4fe14e008e30130b11ede0f0f94a87982e310cf2e765
     1e7264576d78a3134fbaf7829bc24b1d96017cf2bc046b7cd8b08b5775c33d0c
     38fa94212a419a082e6a6b87a8e2ec4a44dd327d7069b85892a707e3fc818544
     c36785c423ec7e0422b2af7364a7ba4da6146cbba7981a0951fcc3fa0430c409
     dcad7101795e4206e637d9358a818e5c32e13b349e62b00bf05cd5a4343ea513
     ```
  
  4. 检查容器的大小
  
     ```shell
     $ sudo du -sh /var/lib/docker/containers/*
     
     32K  /var/lib/docker/containers/1a174fc216cccf18ec7d4fe14e008e30130b11ede0f0f94a87982e310cf2e765
     32K  /var/lib/docker/containers/1e7264576d78a3134fbaf7829bc24b1d96017cf2bc046b7cd8b08b5775c33d0c
     32K  /var/lib/docker/containers/38fa94212a419a082e6a6b87a8e2ec4a44dd327d7069b85892a707e3fc818544
     32K  /var/lib/docker/containers/c36785c423ec7e0422b2af7364a7ba4da6146cbba7981a0951fcc3fa0430c409
     32K  /var/lib/docker/containers/dcad7101795e4206e637d9358a818e5c32e13b349e62b00bf05cd5a4343ea513
     ```
  
     cow策略不仅仅节省空间,也会降低启动时间,当你启动容器(来自同一个进行的多个容器)的时候,docker仅仅需要创建可写容器层即可.
  
     如果docker每次启动都需要对底层镜像进行完全的拷贝,容器的启动时间和磁盘占用了将会大量上升.这个与虚拟机类似,每个虚拟机有一个或者多个虚拟磁盘.

2. 存储驱动器的选择

   理想状态下，非常少的数据会写到可写层，且使用docker数据卷去写数据。但是一些工作负载需要去写数据到容器可写层，这就引入了存储驱动器。

   docker支持多种存储驱动器，使用可插拔的构建。存储驱动器控制镜像和容器的存储方式，并在docker主机上进行管理。
   
   在读完存储驱动器概述之后，下一步就是选择最优的存储驱动器，用于工作负载。做决定是，有三个因素需要考虑：
   
   如果内核支持多个存储驱动器，docker在没有驱动器显示的配置的时候，会有一个优先化的存储驱动器列表。
   
   按照最高性能和稳定性使用存储驱动器是通常的情景。
   
   docker支持下述驱动器：
   
   - `overlay2`是优选的存储驱动器,支持linux,不需要额外的配置
   - `aufs`在18.06以及之前不得版本是最优驱动器.
   - `devicemapper`允许支持,但是在生产环境下需要`direct-lvm`,由于`loopback-lvm`的原因,当没有配置的时候,性能非常差.`devicemapper`在CentOS和RHEL的环境下是推荐驱动器.因为其内核版本不支持`overlay2`.当时当前版本的CentOS和RHEL已经开始支持`overlay2`了,且是推荐的驱动器.
   - `btrfs`和`zfs`存储驱动器在它们作为后备文件系统的时候使用.这些文件系统允许高级配置,比如说创建**快照**,但是需要更多的支持才能够进行.且需要每个后备系统配置正确.
   - `vfs`存储驱动器适用于测试环境下,这种情况可以使用非coW策略的文件系统.存储驱动器的性能很差,不推荐生产环境下使用.
   
   docker源码中定义了选择策略,可以参考[docker引擎的源码](https://github.com/docker/docker-ce/blob/19.03/components/engine/daemon/graphdriver/driver_linux.go#L50),如果运行不同版本的docker,可以使用峰值管理切换到不同的版本下.
   
   一些存储驱动器需要使用指定后备文件系统的形式.如果需要使用指定的后备文件系统的需求,这个会限制你的操作.
   
   当你收缩选择的存储驱动器之后，可以根据工作负载的特这和你需要的稳定性去抉择。参考选择驱动器的[考虑原则](http://127.0.0.1:4000/storage/storagedriver/select-storage-driver/#other-considerations).
   
   > 注意: 你的选择也需要考虑docker版本,操作系统和发行版的情况.例如`aufs`支持Unbuntu和Debian,且需要安装其他包.而`brtfs`仅仅支持SLES,这个只有docker才能支持.

+ 每种linux发行版中存储驱动器的支持情况

  在高等级情况下，存储驱动器可以使用docker版本的部分内容。

  此外，docker不建议需要取消操作系统安全特征的配置、比如说在centOS上使用`overlay`或者`overlay2`的情况下取消`selinux`.

+ docker引擎 - docker企业开发版

  docker引擎的企业开发版中，支持存储驱动器的是产品相容性矩阵。为了获取docker的商业支持，必须使用配置的属性

+ docker引擎 - 社区版

  在社区版本中，一些配置是用于测试的，你的操作系统内核可能不支持所有的存储驱动器。总体而言，线束配置在最近版本的linux发行版中可以使用：

  | linux发行版 | 推荐使用的存储驱动器                | 可替代的存储驱动器                             |
  | ----------- | ----------------------------------- | ---------------------------------------------- |
  | Ubuntu      | `overlay`或者`aufs`                 | `overlay`<br />`devicemapper`<br />`zfs`,`vfs` |
  | Debain      | `overlay2`,`aufs`或者`devicemapper` | `overlay`,`vfs`                                |
  | CentOS      | `overlay2`                          | `overlay`<br />`devicemapper`<br />`zfs`,`vfs` |
  | Fedora      | `overlay2`                          | `overlay`,`devicemapper`<br />`zfs`或者`vfs`   |

  可能的话,使用`overlay2`作为存储驱动器,当首次安装docker时,默认使用`overlay2`,之前的版本,使用`aufs`作为默认banbe,但是现在的情况不再是这样,如果需要使用`aufs`则需要进行配置,并安装额外的安装包.
  
  使用`aufs`安装的应用仍然可以使用.
  
  当你疑惑的时候,最好的配置时使用现在的linux发行版内核支持的`overlay2`驱动器,使用docker数据卷去写出重负载.而不是将数据写入到容器可写层中.
  
  `vfs`通常不是最优选择,在使用`vfs`之前,需要确认这个驱动器的性能问题.
  
  注意： 在Mac或者Windows上修改存储驱动器是不可能的。
  
+ 支持的后备文件系统

  考虑到docker,后备文件系统位于`/var/lib/docker`中,一些存储驱动器仅仅在指定的后备文件系统才能工作.

  | 存储驱动器           | 支持的后备文件系统             |
  | -------------------- | ------------------------------ |
  | `overlay2`,`overlay` | `xfs`(其中ftype=1)<br />`ext4` |
  | `aufs`               | `xfs`,`ext4`                   |
  | `devicemapper`       | `direct-lvm`                   |
  | `btrfs`              | `btrfs`                        |
  | `zfs`                | `zfs`                          |
  | `vfs`                | 任何文件系统                   |

+ 其他考虑

  1.  工作负载的适配

     每个存储驱动器都有自己的性能特征,或多或少会与工作负载相关:

     + `overlay2`,`overlay`和`aufs`操作文件而非数据块,这样可以更加高效的使用内存,但是在重载的情况下,可写层变得比较大.
     + 数据块层级的驱动器`devicemapper`,`brtfs`和`zfs`在重载下性能良好,虽然不如使用数据卷效果好.
     + 对于小写入,且多层的文件系统,`overlay`的效果比`overlay2`多,但是消耗更多的索引节点,这个会导致索引耗尽的问题.
     + `brtfs`和`zfs`需要更多的内存
     + `zfs`在高密度负载,比如说Paas的情况下是更好的选择

     更多关于性能,适配,以及使用的信息参考各个驱动器的使用.

  2. 共享存储系统和存储驱动器

     如果使用SAN,NAS,硬盘RAID,或者其他的共享存储系统,这个可能提供高可用,高性能,副本和压缩的功能.在大多数情况下,docker可以工作在这些存储系统上,但是docker不会紧密的整合它们.

     每个docker存储驱动器基于linux文件系统或者数据卷管理器,确定最受存在的最佳实践去操作存储驱动器.例如,使用ZFS存储驱动器在共享存储系统上,确保遵守ZFS的最佳实践/

  3. 稳定性

     对于一些用户来说,稳定性比性能更重要.进过docker考虑到所有的存储驱动器,一些行的应用仍旧在开发过程中.总体来说,`overlay2`,`aufs`,`overlay`和`devicemapper`是高稳定性的.

  4. 测试工作负载

     在运行不同的负载的时候,可以测试docker的稳定性.确保使用合适的硬件与负载生产条件相匹配,可以查看哪些存储驱动器提供最好的性能.

  5. 检查当前存储驱动器

     每个存储驱动器的详细信息包好了所有的启动信息.

     可以使用`docker info`查看到`Storage Driver`行,查看当前的存储驱动器.

     ```shell
     $ docker info
     
     Containers: 0
     Images: 0
     Storage Driver: overlay2
      Backing Filesystem: xfs
     <output truncated>
     ```

     为了改变存储驱动器,可以查看指定新的存储驱动器教程.一些驱动器需要额外的配置,包括对docker主机物理磁盘和逻辑磁盘的配置.

---

#### **存储器使用教程**


1. 使用AUFS存储驱动器

   [本地教程](http://127.0.0.1:4000/storage/storagedriver/aufs-driver/)

2. 使用Btrfs存储驱动器

   [本地教程](http://127.0.0.1:4000/storage/storagedriver/btrfs-driver/)

3. 使用设备映射存储驱动器

   [本地教程](http://127.0.0.1:4000/storage/storagedriver/device-mapper-driver/)

4. 使用Overlay存储驱动器

   [本地教程](http://127.0.0.1:4000/storage/storagedriver/overlayfs-driver/)

5. 使用ZFS存储驱动器

   [本地教程](http://127.0.0.1:4000/storage/storagedriver/zfs-driver/)

6. 使用VFS存储驱动器

   [本地教程](http://127.0.0.1:4000/storage/storagedriver/vfs-driver/)