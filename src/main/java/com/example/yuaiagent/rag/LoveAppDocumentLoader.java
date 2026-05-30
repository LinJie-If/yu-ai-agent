package com.example.yuaiagent.rag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LoveAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver){
        this.resourcePatternResolver = resourcePatternResolver;
    }


//    加载多篇 md 文件

    public List<Document> loadMarkdowns(){
        List<Document> allDocuments = new ArrayList<>();
        try {
            //? Resource[] resources是什么，resources又是什么
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources){
                String filename = resource.getFilename();
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .build();
                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(markdownDocumentReader.get());

            }
        } catch (IOException e) {
            log.error("MD 文档加载失败", e);
        }

        return allDocuments;
    }

}

