package com.example.yuaiagent.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 创建上下文查询增强器的工厂
 */
public class LoveAppContextQueryAugmenterFactory {

    public static ContextualQueryAugmenter createInstance() {
        PromptTemplate emptyContentPromptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱款，我只能回答恋爱相关的问题，别的没办法帮到您哦，
                有问题可 以联系编程导航客服 https://codefather.cn
                """);

        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContentPromptTemplate)
                .build();
    }
}
