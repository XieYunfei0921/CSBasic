

#### zookeeper审计日志

zk自从3.6.0版本之后支持审计日志的功能。默认情况下，关闭审计日志。可以使用`audit.enable=true`开启编辑功能.这个在`conf/zoo.cfg`中配置.

审计日志不会记录到zk服务器上,仅仅会在客户端连接的服务器上进行记录.

审计日志获取操作的详细信息,按照kv的形式存储.

| Key   | Value |
| ----- | ----- |
|session | client session id |
下述是所有操作的审计日志,客户端连接到`192.168.1.2`.客户端为`zkcli@hadoop.com`服务端为`zookeeper/192.168.1.3@hadoop.com`.

    user=zookeeper/192.168.1.3 operation=serverStart   result=success
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=create    znode=/a    znode_type=persistent  result=success
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=create    znode=/a    znode_type=persistent  result=failure
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=setData   znode=/a    result=failure
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=setData   znode=/a    result=success
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=setAcl    znode=/a    acl=world:anyone:cdrwa  result=failure
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=setAcl    znode=/a    acl=world:anyone:cdrwa  result=success
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=create    znode=/b    znode_type=persistent  result=success
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=setData   znode=/b    result=success
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=delete    znode=/b    result=success
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=multiOperation    result=failure
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=delete    znode=/a    result=failure
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=delete    znode=/a    result=success
    session=0x19344730001   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=create   znode=/ephemral znode_type=ephemral result=success
    session=0x19344730001   user=zookeeper/192.168.1.3   operation=ephemeralZNodeDeletionOnSessionCloseOrExpire  znode=/ephemral result=success
    session=0x19344730000   user=192.168.1.2,zkcli@HADOOP.COM  ip=192.168.1.2    operation=reconfig  znode=/zookeeper/config result=success
    user=zookeeper/192.168.1.3 operation=serverStop    result=invoked

#### zookeeper审计日志配置

默认情况下,审计日志是关闭的.审计日志通过log4j完成.下述是log4j的配置,位于`conf/log4j.properties`下面.

    #
    # zk audit logging
    #
    zookeeper.auditlog.file=zookeeper_audit.log
    zookeeper.auditlog.threshold=INFO
    audit.logger=INFO, RFAAUDIT
    log4j.logger.org.apache.zookeeper.audit.Log4jAuditLogger=${audit.logger}
    log4j.additivity.org.apache.zookeeper.audit.Log4jAuditLogger=false
    log4j.appender.RFAAUDIT=org.apache.log4j.RollingFileAppender
    log4j.appender.RFAAUDIT.File=${zookeeper.log.dir}/${zookeeper.auditlog.file}
    log4j.appender.RFAAUDIT.layout=org.apache.log4j.PatternLayout
    log4j.appender.RFAAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n
    log4j.appender.RFAAUDIT.Threshold=${zookeeper.auditlog.threshold}
    
    # Max log file size of 10MB
    log4j.appender.RFAAUDIT.MaxFileSize=10MB
    log4j.appender.RFAAUDIT.MaxBackupIndex=10

Change above configuration to customize the auditlog file, number of backups, max file size, custom audit logger etc.

改变参数去自定义审计日志,备份数量,最大文件大小等等.

#### 审计日志的用户

默认情况下有下面4中授权提供者

* IPAuthenticationProvider
* SASLAuthenticationProvider
* X509AuthenticationProvider
* DigestAuthenticationProvider

用户取决于授权的提供者:

* 配置IPAuthenticationProvider 情况下,授权IP为用户
* 配置SASLAuthenticationProvider,客户端是用户
* 配置X509AuthenticationProvider,客户端证书是用户
* 配置DigestAuthenticationProvider,授权用户使用用户 

`org.apache.zookeeper.server.auth.AuthenticationProvider.getUserName`设置自动义的授权者.如果没有重写则会采用`org.apache.zookeeper.data.Id.id`作为用户.总体来说,用户以属性的形式存储,取决于授权提供者.

如果zk的部分操作是在服务器上进行的.例如客户端关闭会话,临时节点删除的工作就是服务器完成的.这个删除不是由客户端处理的.对于系统来说,zk服务器的操作被认作审计日志记录的范围.

例如,在zk服务端中`zookeeper/hadoop.hadoop.com@HADOOP.COM`是首选客户端.然后它变成系统用户,所有系统操作都会使用这个用户名称.

	user=zookeeper/hadoop.hadoop.com@HADOOP.COM operation=serverStart result=success

如果没有用户与zk服务器连接,name启动zk服务器的用户视作用户.

	user=root operation=serverStart result=success

单个客户端可以连接到多个授权的会话,这种情况下,授权schema会会被作为用户.用于代表逗号分割的列表.例如,如果客户端使用`zkcli@HADOOP.COM`且ip为`127.0.0.1`那么创建znode的审计日志如下:

	session=0x10c0bcb0000 user=zkcli@HADOOP.COM,127.0.0.1 ip=127.0.0.1 operation=create znode=/a result=success


​	
