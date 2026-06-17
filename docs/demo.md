# yu-ai-agent 项目理解指南

这份文档的目标不是替代网课，而是把这个项目拆成更容易理解的小块。以后代码里遇到看不懂的地方，可以继续用 `// ?` 标出来，再补充到这里。

## 1. 这个项目在做什么

这个项目是一个基于 Spring Boot + Spring AI Alibaba 的 AI 应用。

当前核心能力可以拆成 4 层：

1. Web 层：提供 HTTP 接口，例如健康检查接口。
2. 应用层：封装 AI 对话能力，核心类是 `LoveApp`。
3. AI 增强层：加入记忆、日志、RAG 知识库等 Advisor。
4. 基础设施层：配置大模型、PostgreSQL、Redis、本地文件存储等。

用一句话理解：

用户发消息 -> `LoveApp` 构造 prompt -> ChatClient 调用 DashScope 大模型 -> Advisor 在调用前后增强能力 -> 返回 AI 回复。

## 2. 项目目录怎么读

```text
src/main/java/com/example/yuaiagent
├── YuAiAgentApplication.java        Spring Boot 启动入口
├── controller                       对外 HTTP 接口
├── app                              业务应用入口，封装 AI 对话
├── advisor                          Spring AI Advisor，自定义调用增强
├── chatmemory                       对话记忆存储
├── rag                              RAG 文档加载、向量库、云端知识库配置
└── demo/invoke                      学习用 demo，演示不同方式调用大模型

src/main/resources
├── application.yaml                 主配置
├── application-local.yaml           本地环境配置
└── document                         RAG 本地知识库 markdown 文档
```

## 3. 一次普通对话是怎么跑起来的

以 `LoveApp.doChat(message, chatId)` 为例：

1. 外部调用 `doChat`，传入用户消息和 `chatId`。
2. `chatClient.prompt()` 开始构造一次 AI 请求。
3. `.user(message)` 设置用户输入。
4. `.advisors(...)` 设置本次请求的上下文参数，比如会话 ID 和读取几条历史消息。
5. `.call()` 真正调用大模型。
6. `.chatResponse()` 拿到模型响应。
7. 从 `chatResponse.getResult().getOutput().getText()` 中取出文本。

这里的 `chatId` 很重要。它不是给模型看的，而是给 `ChatMemory` 用的。相同 `chatId` 会读写同一份历史对话，不同 `chatId` 就是不同会话。

## 4. `LoveApp` 里的几个关键对象

### ChatClient

`ChatClient` 是 Spring AI 提供的高层客户端。可以把它理解成“调用大模型的总入口”。

它负责把系统提示词、用户消息、历史记忆、Advisor 等内容组织起来，然后交给底层的 `ChatModel` 调用真实模型。

### ChatModel

构造方法里的 `ChatModel dashscopeChatModel` 是 Spring 自动注入的。

为什么能自动注入？因为项目引入了 Spring AI Alibaba 依赖，并且在配置文件里配置了：

```yaml
spring:
  ai:
    dashscope:
      api-key: ...
      chat:
        options:
          model: qwen-plus
```

Spring Boot 启动时会根据这些配置创建 DashScope 相关的 `ChatModel` Bean。

### Advisor

Advisor 可以理解为“AI 调用链上的拦截器”。

例如：

- `MessageChatMemoryAdvisor`：在调用模型前读取历史消息，调用后保存新消息。
- `MyLoggerAdvisor`：打印用户输入和 AI 输出。
- `QuestionAnswerAdvisor`：先从向量库检索相关文档，再把文档内容补进 prompt，让模型基于知识库回答。

## 5. 代码中 `// ?` 的解释

### 问题 1：`Resource[] resources` 是什么？

位置：`LoveAppDocumentLoader.loadMarkdowns()`

```java
Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
```

这里分成两个概念看：

`Resource` 是 Spring 对“资源文件”的抽象。资源可以来自很多地方，比如：

- classpath 里的文件
- 本地磁盘文件
- URL 网络资源
- jar 包里的文件

在这个项目里，`Resource` 指的是 `src/main/resources/document` 目录下的 markdown 文件。

`Resource[] resources` 是一个数组，因为 `"classpath:document/*.md"` 可能匹配到多篇文档。

例如当前项目里会匹配到：

```text
恋爱常见问题和回答-单身篇.md
恋爱常见问题和问答-恋爱篇.md
恋爱常见问题和回答-已婚篇.md
```

所以可以这样理解：

```text
ResourcePatternResolver
  -> 按 classpath:document/*.md 查找文件
  -> 找到多个 markdown 文件
  -> 每个文件包装成一个 Resource
  -> 返回 Resource[]
```

后面的 `for (Resource resource : resources)` 就是在逐个读取这些 markdown 文件。

### 问题 2：`@Configuration // ?` 是什么？

位置：`LoveAppVectorStoreConfig`

```java
@Configuration
public class LoveAppVectorStoreConfig {
    @Bean
    VectorStore loveAppVectorStore(...) {
        ...
    }
}
```

`@Configuration` 表示这是一个 Spring 配置类。

配置类的作用是告诉 Spring：

这个类里面可能有一些 `@Bean` 方法，请在项目启动时调用这些方法，并把返回对象放进 Spring 容器。

当前这段代码的意思是：

1. Spring 启动时发现 `LoveAppVectorStoreConfig`。
2. 看到它是 `@Configuration` 配置类。
3. 调用 `loveAppVectorStore(...)` 方法。
4. 方法返回一个 `VectorStore`。
5. Spring 把这个 `VectorStore` 保存成一个 Bean。
6. `LoveApp` 里用 `@Resource private VectorStore loveAppVectorStore;` 就可以拿到它。

更口语一点：

`@Configuration` 像是一个“对象工厂说明书”，`@Bean` 方法负责生产对象，Spring 负责保存和注入这些对象。

### 问题 3：`CHAT_MEMORY_CONVERSATION_ID_KEY` 和 `CHAT_MEMORY_RETRIEVE_SIZE_KEY` 是什么？

位置：`LoveApp.doChatWithRag()`

```java
advisors(spec -> spec
        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
```

这两个参数是传给 `MessageChatMemoryAdvisor` 用的。

`CHAT_MEMORY_CONVERSATION_ID_KEY`：告诉记忆组件当前是哪一个会话。

例如：

```text
chatId = "abc"
```

那它就会读取和保存 `abc` 这条会话的历史消息。

`CHAT_MEMORY_RETRIEVE_SIZE_KEY`：告诉记忆组件本次调用模型前，要取最近多少条历史消息。

例如：

```java
.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)
```

意思是最多取最近 10 条消息放进上下文。

所以你写的注释“保存对话记忆，并且加载最近的 10 条”大体是对的，不过更准确一点是：

它给 `MessageChatMemoryAdvisor` 提供参数，让 Advisor 知道“当前会话是谁”和“本次要读取多少条历史消息”。保存动作是 Advisor 在调用后完成的。

### 问题 4：`static { ... }` 是什么写法？

位置：`FileBaseChatMemory`

```java
private static final Kryo kryo = new Kryo();

static {
    kryo.setRegistrationRequired(false);
    kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
}
```

这是 Java 的静态代码块。

它的特点是：

- 属于类本身，不属于某一个对象。
- 在类第一次被 JVM 加载时执行一次。
- 常用来初始化 `static` 静态变量。

在这里，`kryo` 是一个静态对象：

```java
private static final Kryo kryo = new Kryo();
```

然后 `static { ... }` 对这个 Kryo 对象做初始化配置：

`setRegistrationRequired(false)`：告诉 Kryo 不强制要求提前注册所有要序列化的类。学习阶段这样更方便。

`setInstantiatorStrategy(new StdInstantiatorStrategy())`：告诉 Kryo 遇到某些没有无参构造方法的类时，也尽量能创建对象。Spring AI 的消息对象可能不是为 Kryo 专门设计的，所以这里加这个策略更稳。

可以把它理解成：

```text
创建 Kryo 工具
  -> 项目第一次用到 FileBaseChatMemory 类时
  -> 先统一配置这个工具
  -> 后面所有 FileBaseChatMemory 对象共用它
```

### 问题 5：`MyLoggerAdvisor` 里的 `aroundCall` 是什么？

位置：`MyLoggerAdvisor`

```java
public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
    advisedRequest = this.before(advisedRequest);
    AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
    this.observeAfter(advisedResponse);
    return advisedResponse;
}
```

它是同步调用大模型时的“环绕增强”。

执行顺序是：

1. `before(advisedRequest)`：模型调用前执行，当前用于打印用户输入。
2. `chain.nextAroundCall(advisedRequest)`：继续执行后面的 Advisor，最后真正调用大模型。
3. `observeAfter(advisedResponse)`：模型返回后执行，当前用于打印 AI 回复。
4. 返回响应。

所以它像一个夹在模型调用前后的拦截器：

```text
用户请求
  -> MyLoggerAdvisor.before
  -> 下一个 Advisor
  -> ChatModel 调用大模型
  -> MyLoggerAdvisor.observeAfter
  -> 返回结果
```

`aroundStream` 则对应流式输出场景。如果以后用流式响应，就会走它。

## 6. RAG 本地知识库是怎么工作的

当前本地 RAG 链路是：

```text
markdown 文档
  -> LoveAppDocumentLoader 加载文档
  -> MarkdownDocumentReader 切分成 Document
  -> EmbeddingModel 把文本转成向量
  -> VectorStore 保存向量
  -> QuestionAnswerAdvisor 检索相关文档
  -> 把检索结果补给大模型
```

几个关键词：

`Document`：Spring AI 里的文档对象，里面有文本内容和 metadata。

`EmbeddingModel`：把文本转成向量的模型。当前是 DashScope 的 embedding 模型。

`VectorStore`：保存向量并支持相似度搜索的组件。

`QuestionAnswerAdvisor`：提问时先查向量库，找到相关文档，再让模型参考这些文档回答。

## 7. PostgreSQL / pgvector 当前配置的注意点

项目里已经有 PostgreSQL 和 pgvector 相关配置：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/yu_ai_agent
    username: postgres
    password: 123456

  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        dimensions: 1536
        distance-type: COSINE_DISTANCE
        max-document-batch-size: 10000
```

`docker-compose.yml` 里也启动了：

```yaml
image: pgvector/pgvector:pg16
ports:
  - "5433:5432"
```

这表示：

- 容器内部 PostgreSQL 端口是 `5432`。
- 你本机访问它要用 `localhost:5433`。
- 数据库名是 `yu_ai_agent`。
- 用户名是 `postgres`。
- 密码是 `123456`。

但是当前 `LoveAppVectorStoreConfig` 手动创建的是：

```java
SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
```

`SimpleVectorStore` 是内存向量库，不是 PostgreSQL。

也就是说：你虽然配置了 PostgreSQL/pgvector，但当前 `loveAppVectorStore` 这个 Bean 实际还在用内存版向量库。

如果后面要真正使用 PostgreSQL pgvector，需要把 `VectorStore` 切换到 Spring AI 自动创建的 `PgVectorStore`，或者自己创建 `PgVectorStore` Bean。

另外，pgvector 常用配置里建议补上：

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true
```

它的作用是让 Spring AI 启动时自动初始化向量表结构。学习阶段可以先开着，方便验证；正式环境要更谨慎。

## 8. 当前项目里几个容易混淆的点

### 内存向量库 vs PostgreSQL 向量库

`SimpleVectorStore`：

- 数据存在 Java 内存里。
- 项目重启后通常就没了。
- 适合学习和快速 demo。

`PgVectorStore`：

- 数据存在 PostgreSQL 里。
- 项目重启后数据还在。
- 更接近真实项目。

### 本地文件记忆 vs Redis / 数据库记忆

当前聊天记忆使用的是：

```java
ChatMemory chatMemory = new FileBaseChatMemory(fileDir);
```

它会把消息序列化到：

```text
tmp/chat-memory/*.kryo
```

所以虽然项目配置了 Redis，但目前聊天记忆并没有使用 Redis。

### 配置了依赖不等于代码已经用上

这是学习 Spring 项目时很关键的一点：

`pom.xml` 里引入依赖，只表示“项目具备这个能力”。

真正用没用上，还要看：

- 有没有配置属性
- 有没有自动配置生效
- 有没有对应 Bean 被注入
- 业务代码里最终调用的是哪个实现类

当前 PostgreSQL/pgvector 就是典型例子：依赖和配置已经有了，但业务注入的 `loveAppVectorStore` 仍然来自手写的 `SimpleVectorStore`。

## 9. 以后遇到看不懂的代码怎么提问

建议你继续在代码旁边写：

```java
// ? 这里为什么要这样写？
```

或者更具体一点：

```java
// ? 这个对象是谁创建的？
// ? 这个参数传给谁用？
// ? 这里为什么不是 new 出来？
// ? 这个配置和代码在哪里对应？
// ? 这个 Bean 最后被谁注入？
```

我后面可以按这个顺序帮你讲：

1. 这行代码的直接含义。
2. 它在 Spring / Spring AI 里的角色。
3. 它和项目其他文件的连接关系。
4. 如果删掉或改错，会出现什么问题。
5. 用一个生活化例子重新解释。

## 10. 下一步建议整理的文档

建议后面继续补 4 份文档：

1. `docs/spring-basics-for-this-project.md`：只讲本项目用到的 Spring 注解、Bean、依赖注入。
2. `docs/chat-memory-guide.md`：讲聊天记忆、`chatId`、Kryo 文件存储。
3. `docs/rag-guide.md`：讲 markdown 文档、Document、Embedding、VectorStore、QuestionAnswerAdvisor。
4. `docs/postgresql-pgvector-guide.md`：讲 Docker、PostgreSQL、pgvector、Spring AI PgVectorStore。

不要一口气吃完整个项目。这个项目看起来庞大，是因为它把 Spring Boot、大模型、RAG、向量数据库、文件存储、测试都放到了一起。我们把它拆开，每次只搞懂一条链路，就会舒服很多。
