# Day1 学习总结

今天主要跟着网课完成了 AI Agent 项目的第一天入门内容，重点不是写复杂业务，而是先把项目跑起来，理解 Spring Boot 项目如何接入大模型，以及本地开发环境里常见问题怎么排查。

## 今天学了什么

### 1. 创建并启动 Spring Boot 项目

今天先熟悉了一个基础 Spring Boot 项目的结构：

- `pom.xml`：管理项目依赖，比如 Spring Boot、DashScope SDK、Spring AI Alibaba 等。
- `src/main/java`：放 Java 源码。
- `src/main/resources`：放配置文件，比如 `application.yaml`、`application-local.yaml`。
- `YuAiAgentApplication`：项目启动类。

也学到一点：只要 Spring Boot 正常启动，控制台一般会看到类似：

```text
Tomcat started on port ...
Started YuAiAgentApplication
```

如果启动失败，要重点看最后的 `Caused by`，那里通常才是真正原因。

### 2. 学习了几种调用 AI 的方式

今天接触了几种调用大模型的方式：

- 直接用 HTTP 请求调用接口。
- 使用 DashScope 官方 SDK 调用。
- 使用 Spring AI / Spring AI Alibaba 调用。

简单理解：

- HTTP 方式更底层，适合理解请求是怎么发出去的。
- SDK 方式封装更多，用起来更方便。
- Spring AI 方式更适合以后放进正式 Spring Boot 项目里，方便和 Controller、Service、配置文件结合。

### 3. 学习了配置文件和多环境配置

今天用到了两个配置文件：

- `application.yaml`
- `application-local.yaml`

`application.yaml` 是主配置文件，`application-local.yaml` 是本地环境配置文件。

如果想让 Spring Boot 加载本地配置，需要在主配置里写：

```yaml
spring:
  profiles:
    active: local
```

注意是 `profiles`，有 `s`。

### 4. 学习了 API Key 的配置方式

DashScope 调用大模型时必须配置 API Key。

可以写在配置里：

```yaml
spring:
  ai:
    dashscope:
      api-key: 你的 API Key
```

但更推荐以后写成环境变量：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
```

然后在 IDEA 的运行配置里配置：

```text
DASHSCOPE_API_KEY=你的 API Key
```

这样不会把真实 key 写死在代码里，也不容易误提交。

## 今天遇到的问题和解决过程

### 问题 1：Mac 上执行 `curl` 看起来“没反应”

网课里老师用的是 Windows PowerShell：

```powershell
curl https://codefather.cn
```

PowerShell 里的 `curl` 很多时候其实是 `Invoke-WebRequest` 的别名，所以会显示：

```text
StatusCode
StatusDescription
Content
Headers
```

但 Mac 里的 `curl` 是真正的 curl，默认行为不一样，所以看起来和网课不一致。

解决方式：

```bash
curl -I https://codefather.cn
```

用它可以看响应状态码和响应头。

也可以用：

```bash
curl -v https://codefather.cn
```

查看更详细的请求过程。

### 问题 2：Spring Boot 启动失败，提示 DashScope API Key 没设置

报错核心信息是：

```text
DashScope API key must be set.
Use the connection property:
spring.ai.dashscope.api-key
or
spring.ai.dashscope.chat.api-key
```

一开始以为是 IDEA 没读到环境变量，后来检查配置文件后发现真正原因是：

```yaml
spring:
  profile:
    active: local
```

这里写成了 `profile`，少了一个 `s`。

正确写法是：

```yaml
spring:
  profiles:
    active: local
```

因为这个单词写错，Spring Boot 没有激活 `local` 环境，所以 `application-local.yaml` 没有被加载，里面的 API Key 自然也没有生效。

### 问题 3：日志很多，不知道该看哪里

启动失败时，控制台会打印很多内容，比如：

- Nacos 日志
- SLF4J 警告
- Commons Logging 警告
- Tomcat 启动信息

今天学到：不要被一大堆日志吓住，优先找：

```text
Caused by:
```

通常最后一个 `Caused by` 最接近真正原因。

这次真正原因就是 DashScope API Key 没加载。

## 今天的关键收获

1. Mac 和 Windows 的命令行行为可能不一样，尤其是 `curl`。
2. Spring Boot 配置文件里单词和缩进都很重要。
3. `spring.profiles.active` 必须写对，否则环境配置不会加载。
4. 大模型 API Key 不建议直接写死在代码里。
5. 看 Java 报错时，不要从第一行开始慌，先找最后的 `Caused by`。
6. 第一天的重点不是写复杂代码，而是把项目启动、配置、大模型调用链路跑通。

## 明天可以继续做什么

明天可以继续围绕 Day1 的基础往下走：

- 确认 Spring Boot 项目能稳定启动。
- 确认 DashScope 调用能正常返回结果。
- 理解 HTTP 调用、SDK 调用、Spring AI 调用的区别。
- 给关键代码加简单注释，保证之后回来看得懂。

今天整体算是把环境、配置和第一个 AI 调用入口摸清楚了。这个阶段踩坑很正常，而且这些坑都很典型：以后做自己的复刻项目时，也会经常遇到类似的配置加载、环境变量、日志排查问题。
