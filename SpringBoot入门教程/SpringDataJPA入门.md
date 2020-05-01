##### Spring Data JPA使用说明:
---

1. REST端口的使用
注解`@RepositoryRestResource` 用于引导SpringMVC创建RESTful后台
spring boot自动JPA实现`PersonRepository`,且使用JPA配置后台内存数据库.

Spring Data Rest建立在Spring MVC上,创建了一个SpringMVC集合,json转换器,且会创建
bean(提供rest后台). 这些组件会连接到spring data jpa后台,这些都是自动配置的.如果需要
研究如何使用,查看`@RepositoryRestMvcConfiguration`的使用.

发送curl(WSL):
```shell
curl -i -H "Content-Type:application/json" -d '{"name": "Frodo", "age": 18,"address":"127.0.0.1"}' http://localhost:8080/people HTTP/1.1 201 Created
``` 
发送curl(非wsl的windows)
```shell
curl -i -H "Content-Type:application/json" -d "{\"name\": \"Frodo\", \"age\": 18,\"address\":\"127.0.0.1\"}" http://localhost:8080/people HTTP/1.1 201 Created
```

查找定义的查询
```shell
curl http://localhost:8080/people/search
```
结果如下:
```markdown
"_links" : {
    "findByLastName" : {
      "href" : "http://localhost:8080/people/search/findByLastName{?name}",
      "templated" : true
    }
  }
```

查找记录
```markdown
curl http://localhost:8080/people/search/findByLastName?name=Baggins
```

+ 可以使用`PUT`,`PATCH`,`DELETE`REST指令去替换,更新,删除指定的记录.

`PUT`:(更新完全的记录)
```markdown
curl -X PUT -H "Content-Type:application/json" -d '{"firstName": "Bilbo", "lastName": "Baggins"}' http://localhost:8080/people/1
```
windows非wsl
```markdown
curl -X PUT -H "Content-Type:application/json" -d "{\"name\": \"Bilbo\", \"age\": 24,\"address\":\"192.168.0.1\"}" http://localhost:8080/people/1
```

`PATCH`:(更新子集时间)
```markdown
curl -X PATCH -H "Content-Type:application/json" -d "{\"name\": \"Katty\", \"age\": 21,\"address\":\"100.47.53.67\"}" http://localhost:8080/people/1
```

`DELETE`(删除记录)
```markdown
curl -X DELETE http://localhost:8080/people/1
```