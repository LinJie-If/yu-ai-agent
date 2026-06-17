**Day4 RAG 知识库基础**

```md
# DayX 项目学习包：章节名

## 1. 本章一句话目标
这一章到底让项目新增了什么能力？

## 2. 本章业务场景
从一个具体用户问题开始：
用户输入什么？
系统应该返回什么？
为什么普通 AI 回答不够？

## 3. 本章在项目中的位置
它属于项目哪一层？
和前面章节、后面章节是什么关系？

## 4. 本章核心流程
用自然语言描述一次业务是怎么跑的。

## 5. 涉及的代码文件
每个文件回答：
- 它是什么
- 为什么需要它
- 它什么时候发挥作用
- 如果没有它会怎样

## 6. 核心概念解释
每个概念固定写：
- 是什么
- 为什么需要
- 本项目怎么用
- 我自己的理解

## 7. 用户发消息后的代码链路
按执行顺序列：
用户消息 -> 哪个方法 -> 哪个对象 -> 做了什么 -> 下一个环节

## 8. 项目启动时提前做了什么
哪些代码不是用户发消息时才执行，而是启动时就准备好了？

## 9. 白纸复现图
给自己画图用，不要求好看，只要求能讲。

## 10. 我最容易误解的点
列出容易混淆的概念。

## 11. 问题记录
分为：
- A 类：阻塞主流程，立刻解决
- B 类：影响细节，稍后解决
- C 类：进阶内容，先记录

## 12. 自测题
分初级、中级、高级。

## 13. 面试表达
如果面试官问“你项目里的这一章做了什么”，我怎么讲？
```

下面我用 Day4 试填一版。

---

# Day4 项目学习包：RAG 知识库基础

## 1. 本章一句话目标

让 AI 恋爱大师不只是凭大模型自己的通用知识回答，而是能先查询我们准备好的恋爱知识文档，再结合文档内容回答用户问题。

一句话：rag知识库让项目有了“基于知识库回答问题”的能力。

```text

Day4 给项目加上了“基于知识库回答问题”的能力。
```

## 2. 本章业务场景

用户输入：

```text
我已经结婚了，但是婚后关系不太亲密，怎么办？
```

普通 AI 也能回答，但它的问题是：

```text
它可能只是凭通用经验回答。
它不知道我们项目里准备的恋爱问答文档。
它不会主动引用我们的知识库内容。
```

RAG 要解决的是：

```text
回答前先查资料，再让 AI 基于资料回答。
```

## 3. 本章在项目中的位置

Day4 接在 Day3 后面。

Day3 主要是：

```text
AI 对话
Prompt
ChatClient
对话记忆
Advisor
结构化输出
```

Day4 在 Day3 的基础上加：

```text
知识库检索
```

所以 Day4 的位置是：

```text
普通 AI 恋爱聊天
-> 带本地知识库的 AI 恋爱问答
```

Day5 则是在 Day4 基础上继续优化：

```text
文档切分
元数据
PgVector
查询重写
自定义检索增强
```

你现在学 Day4 时，先不要被 Day5 带跑。Day4 先抓最小闭环。

## 4. 本章核心流程

Day4 可以拆成两个阶段。

第一阶段：项目启动时准备知识库。

```text
Markdown 恋爱文档
-> LoveAppDocumentLoader 读取文档
-> 转成 Document
-> EmbeddingModel 把文本转成向量
-> VectorStore 保存向量
```

第二阶段：用户提问时使用知识库。

```text
用户提问
-> LoveApp.doChatWithRag
-> ChatClient 构造请求
-> QuestionAnswerAdvisor 查询 VectorStore
-> 找到相关 Document
-> 把 Document 内容拼进 Prompt
-> 大模型生成回答
```

这是 Day4 的核心。

## 5. 涉及的代码文件

### `src/main/resources/document/*.md`

它是什么：

```text
恋爱知识库的原始资料。
```

为什么需要：

```text
RAG 必须有外部知识来源，否则 AI 没东西可查。
```

什么时候发挥作用：

```text
项目启动初始化知识库时，被 LoveAppDocumentLoader 读取。
```

如果没有它：

```text
VectorStore 里没有知识，QuestionAnswerAdvisor 查不到有用内容。
```

### `LoveAppDocumentLoader`

它是什么：

```text
文档加载器。
```

为什么需要：

```text
Markdown 文件只是普通文件，Spring AI 不能直接拿它做 RAG。
需要先把 Markdown 读取成 Spring AI 里的 Document。
```

它什么时候发挥作用：

```text
初始化 VectorStore 时调用 loadMarkdowns()。
```

它做了什么：

```text
找到 classpath:document/*.md
逐个读取 Markdown
把每篇文档切成 Document
给 Document 加 metadata
返回 List<Document>
```

你可以这样理解：

```text
LoveAppDocumentLoader = 把知识库原材料搬进系统的人。
```

---

### `LoveAppVectorStoreConfig`

它是什么：

```text
向量库配置类，负责创建 VectorStore。
```

为什么需要：

```text
Document 只是文本对象，还不能高效做语义检索。
需要放进 VectorStore，后续用户提问时才能按语义相似度搜索。
```

它什么时候发挥作用：

```text
Spring 启动创建 Bean 时。
```

它做了什么：

```text
创建 SimpleVectorStore
调用 LoveAppDocumentLoader 加载文档
把文档 add 到 VectorStore
```

你可以这样理解：

```text
LoveAppVectorStoreConfig = 建知识库仓库，并把文档放进去。
```

注意：你当前项目里的代码已经混入 Day5 的内容，比如 `MyKeywordEnricher`，但 Day4 先只理解“文档放入 VectorStore”这件事。

---

### `LoveApp.doChatWithRag`

它是什么：

```text
RAG 对话入口。
```

为什么需要：

```text
普通 doChat 只是聊天。
doChatWithRag 是带知识库检索的聊天。
```

它什么时候发挥作用：

```text
用户发起一次 RAG 问答时。
```

它做了什么：

```text
接收用户 message
构造 ChatClient 请求
设置用户输入
设置对话记忆参数
加入日志 Advisor
加入 QuestionAnswerAdvisor
调用大模型
返回回答
```

---

### `QuestionAnswerAdvisor`

它是什么：

```text
Spring AI 提供的 RAG 问答增强器。
```

为什么需要：

```text
它负责在调用大模型前，先去 VectorStore 查相关文档。
```

它什么时候发挥作用：

```text
用户调用 doChatWithRag 时。
```

它做了什么：

```text
拿用户问题
-> 查询 VectorStore
-> 找相关 Document
-> 把 Document 内容拼进 Prompt
-> 让模型基于这些内容回答
```

你可以这样理解：

```text
QuestionAnswerAdvisor = 帮 AI 查小抄的人。
```

## 6. 核心概念解释

### RAG

是什么：

```text
Retrieval-Augmented Generation，检索增强生成。
```

为什么需要：

```text
大模型可能不知道项目私有知识，也可能胡编。
RAG 让模型回答前先检索外部知识。
```

本项目怎么用：

```text
检索恋爱知识文档，再让 AI 回答恋爱问题。
```

我的理解：

```text
RAG 不是训练模型，而是在回答前给模型递资料。
```

---

### Document

是什么：

```text
Spring AI 表示一段文档内容的对象。
```

为什么需要：

```text
RAG 不能直接处理一整个 Markdown 文件，需要把文件内容变成统一的 Document。
```

本项目怎么用：

```text
LoveAppDocumentLoader 把 Markdown 读成 List<Document>。
```

我的理解：

```text
Document 是知识库里的一个知识片段。
```

---

### EmbeddingModel

是什么：

```text
把文本转成向量的模型。
```

为什么需要：

```text
计算机不能直接理解“这句话和那段文档语义相近”，所以要转成向量再算相似度。
```

本项目怎么用：

```text
VectorStore 添加 Document 时，会通过 EmbeddingModel 生成向量。
```

我的理解：

```text
EmbeddingModel 把文字翻译成数学坐标。
```

---

### VectorStore

是什么：

```text
向量存储，负责保存向量并支持相似度搜索。
```

为什么需要：

```text
用户提问时，需要从很多文档里快速找出最相关的片段。
```

本项目怎么用：

```text
Day4 使用 SimpleVectorStore。
```

我的理解：

```text
VectorStore 是 AI 的知识库仓库。
```

---

### SimpleVectorStore

是什么：

```text
Spring AI 提供的内存版向量库。
```

为什么需要：

```text
学习阶段简单，不需要先搭复杂数据库。
```

本项目怎么用：

```text
LoveAppVectorStoreConfig 创建它并写入文档。
```

我的理解：

```text
SimpleVectorStore 是临时知识库，方便先跑通流程。
```

---

## 7. 用户发消息后的代码链路

以用户输入为例：

```text
我已经结婚了，但是婚后关系不太亲密，怎么办？
```

流程：

```text
1. 用户消息进入 LoveApp.doChatWithRag(message, chatId)

2. ChatClient.prompt()
   开始构造一次 AI 请求。

3. .user(message)
   把用户问题放进请求里。

4. .advisors(...)
   设置对话记忆参数，比如 chatId 和读取最近几条消息。

5. new MyLoggerAdvisor()
   打印请求和响应日志，方便观察。

6. new QuestionAnswerAdvisor(loveAppVectorStore)
   这是 RAG 核心。
   它拿用户问题去 loveAppVectorStore 里查相关文档。

7. VectorStore 根据语义相似度返回相关 Document。

8. QuestionAnswerAdvisor 把 Document 内容拼进 Prompt。

9. ChatClient.call()
   调用 DashScope 大模型。

10. chatResponse.getResult().getOutput().getText()
    取出 AI 最终回答。
```

## 8. 项目启动时提前做了什么

这点很重要。

不是用户发消息时才读取 Markdown。

Day4 有一部分工作是在 Spring Boot 启动时提前准备的：

```text
启动项目
-> Spring 创建 LoveAppVectorStoreConfig
-> 创建 VectorStore Bean
-> 调用 LoveAppDocumentLoader.loadMarkdowns()
-> 读取 Markdown 文档
-> 转成 Document
-> 写入 VectorStore
```

所以用户发消息时，知识库已经准备好了。

用户发消息时只是：

```text
去已经准备好的 VectorStore 里查。
```

## 9. 白纸复现图

你可以在白纸上画两个框。

第一块：启动阶段。

```text
【项目启动阶段】

document/*.md
      ↓
LoveAppDocumentLoader
      ↓
List<Document>
      ↓
EmbeddingModel
      ↓
向量
      ↓
VectorStore
```

第二块：用户提问阶段。

```text
【用户提问阶段】

用户问题
  ↓
LoveApp.doChatWithRag
  ↓
ChatClient
  ↓
QuestionAnswerAdvisor
  ↓
VectorStore 相似度检索
  ↓
相关 Document
  ↓
拼进 Prompt
  ↓
DashScope 大模型
  ↓
AI 回答
```

你画的时候，只要能讲出这两句话就行：

```text
启动时先建知识库。
提问时再查知识库。
```

## 10. 我最容易误解的点

### 误解 1：RAG 是训练模型

不是。

```text
RAG 不训练模型。
RAG 是在模型回答前查资料。
```

### 误解 2：Markdown 文件直接给模型看

不是。

```text
Markdown 先变成 Document。
Document 再变成向量。
向量存进 VectorStore。
```

### 误解 3：VectorStore 里只存文字

不准确。

```text
它主要用于存向量，同时保留对应的文档内容和 metadata。
```

### 误解 4：QuestionAnswerAdvisor 是大模型

不是。

```text
它是调用大模型前的增强器。
真正生成回答的还是 ChatModel / DashScope。
```

### 误解 5：Day4 就要搞懂 PgVector

不用。

```text
Day4 先理解 SimpleVectorStore。
PgVector 是 Day5 的持久化向量库方案。
```

## 11. 问题记录

### A 类：阻塞主流程，立刻解决

```text
QuestionAnswerAdvisor 到底做什么？
VectorStore 里到底存什么？
Document 是怎么来的？
为什么用户问题能匹配到文档？
```

### B 类：影响细节，稍后解决

```text
MarkdownDocumentReaderConfig 每个参数什么意思？
metadata 有什么用？
SimpleVectorStore 数据什么时候消失？
```

### C 类：进阶内容，先记录

```text
PgVector
HNSW
查询重写
多查询扩展
自定义 RetrievalAugmentationAdvisor
元数据过滤
```

## 12. 自测题

### 初级

```text
1. Day4 给项目新增了什么能力？
2. RAG 是为了解决什么问题？
3. LoveAppDocumentLoader 的作用是什么？
4. Document 是什么？
5. VectorStore 是什么？
```

### 中级

```text
1. 为什么 Markdown 文件不能直接等于知识库？
2. EmbeddingModel 在 RAG 中起什么作用？
3. QuestionAnswerAdvisor 为什么能让 AI 参考知识库？
4. SimpleVectorStore 适合什么场景？
5. 用户发消息时，知识库是临时创建的吗？
```

### 高级

```text
1. 如果没有 QuestionAnswerAdvisor，doChatWithRag 和普通 doChat 有什么区别？
2. 为什么 RAG 可以减少大模型幻觉？
3. 为什么文档切分会影响检索效果？
4. 如果用户问的问题和知识库无关，会发生什么？
5. Day4 的 SimpleVectorStore 和 Day5 的 PgVector 有什么区别？
```

## 13. 面试表达

如果面试官问：

```text
你这个项目里的 RAG 是怎么实现的？
```

你可以这样讲：

```text
我在 AI 恋爱大师项目里实现了一个基础 RAG 知识库问答能力。

首先，我把恋爱相关的 Markdown 问答文档放在 resources/document 目录下。项目启动时，通过 LoveAppDocumentLoader 使用 Spring AI 的 MarkdownDocumentReader 读取这些文档，并转换成 Document 对象。

然后，通过 VectorStore 配置类创建 SimpleVectorStore，把这些 Document 写入向量库。写入过程中会借助 EmbeddingModel 把文本转换成向量，方便后续做语义相似度检索。

用户提问时，会进入 LoveApp 的 doChatWithRag 方法。这个方法通过 ChatClient 构造 AI 请求，并添加 QuestionAnswerAdvisor。QuestionAnswerAdvisor 会先根据用户问题去 VectorStore 检索相关文档，再把检索结果作为上下文拼接到 Prompt 中，最后让大模型基于这些上下文生成回答。

所以这个功能的核心不是训练模型，而是在模型回答前增加了一步知识库检索，让回答更贴合项目自己的恋爱知识文档。
```

你接下来就照这个方式学：先拿这份 Day4 学习包对照项目看一遍，然后在白纸上只画“启动阶段”和“用户提问阶段”两张图。画完后，不要急着背概念，先尝试用自己的话讲 3 分钟。讲不顺的地方，就是下一轮要补的知识洞。
