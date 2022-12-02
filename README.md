
## 环境安装及启动
所需环境
- JDK-11
- Maven 3.8.1
- MySQL 5.7

Java 环境安装和Maven安装跳过，自己折散

安装MySQL 5.7
```dockerfile
docker run -d -p 3306:3306  --name mysqldb -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=prestogateway -d mysql:5.7
```

MySQL 初始化创建表：进入创建好的MySQL，
在 prestogateway 下执行 src/main/resources/gateway-ha-persistence.sql

使用 Maven 下载项目的依赖包
```mvn
cd trino-gateway
mvn clean install
```

修改分流配置文件路径：

gateway-ha/gateway-ha-config.yml 里的 routingRules.rulesConfigPath 设置为 gateway-ha/src/main/resources/rules/routing_rules.yml 的绝对路径

启动项目
```shell
java -jar gateway-ha-1.9.5-jar-with-dependencies.jar server ../gateway-ha-config.yml
```
浏览器访问Web端：http://localhost:8080/ ，运行成功

使用 IDEA 启动项目

运行 src/main/java/com/lyft/data/gateway/ha/HaGatewayLauncher.java 文件，
添加运行参数
```text
server $FileDir$/gateway-ha-config.yml
```
ps: 如果启动报错，请先用 shell 方式启动一次项目，访问Web端，成功后才可以用IDEA启动。

## 快速上手

本地部署俩个 Trino，用于快速测试路由分流功能

```dockerfile
dokcer-compose up -d
```

Gateway 添加这俩 Trino Backend
```curl
curl --location --request POST 'http://localhost:8080/entity?entityType=GATEWAY_BACKEND' \
--header 'Content-Type: application/json' \
--data-raw '{
    "name": "trino1",
    "proxyTo": "http://localhost:9090",
    "active": true,
    "routingGroup": "trino1"
}'

curl --location --request POST 'http://localhost:8080/entity?entityType=GATEWAY_BACKEND' \
--header 'Content-Type: application/json' \
--data-raw '{
    "name": "trino2",
    "proxyTo": "http://localhost:9091",
    "active": true,
    "routingGroup": "trino2"
}'
```

添加完成后，打开 http://localhost:8080/viewgateway 可以看到新增的 Backends

使用数据库客户端连接 Trino-Gateway 

这里使用的是 Dbeaver 连接，选择 Trino 驱动连接 Trino-Gateway
```yml
host: localhost
port: 8080
user: any
```

连接成功后，可以看到 Trino 自带的一些库和表信息，发起查询后，在 http://localhost:8080/ 可以看到历史查询

## 如何分流
定义分流规则  src/main/resources/rules/routing_rules.yml

```yaml
name: "trino"
description: "trino group"
compositeRuleType: "ActivationRuleGroup"
composingRules:
  - name: "trino1"
    description: "route to trino1"
    condition: "request.getAttribute(\"sql\") contains \"runtime.queries\""
    actions:
      - "result.put(\"routingGroup\", \"trino1\")"
  - name: "trino2"
    description: "route to trino2"
    condition: "request.getHeader(\"X-Trino-Source\") == \"trino-jdbc\""
    actions:
      - "result.put(\"routingGroup\", \"trino2\")"
```
上述规则简单解释一下：
- 匹配 SQL 里含有 runtime.queries 的查询，分派给 Trino1
- 匹配 Http Header X-Trino-Source 为 trino-jdbc，分派给 Trino2

例如使用 dbeaver 执行 sql
```sql
select * from "system".runtime.queries 
```

规则匹配是由上而下匹配，命中则分派到对应 Trino。规则可以动态编写，不需要重启项目。

规则规则是由 MVEL 语言编写，可以根据 Http Request 内容进行自定义规则，规则的定义灵活度和覆盖度很高

路由规则详情请看原文档：Routing Rules Engine 部分
