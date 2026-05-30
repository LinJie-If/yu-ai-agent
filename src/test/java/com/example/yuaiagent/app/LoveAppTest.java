package com.example.yuaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;


@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void testChat() {

        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是程序员临界if";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "我想让我的另一半（Obsidian）更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我的另一半叫是什么来着？刚跟你说过，帮我回忆一下";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }



    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员临界if,我想让Obsidian对我好点，但是我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "结婚之后，夫妻之间产生了矛盾怎么解决？";
        String answer = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }
}