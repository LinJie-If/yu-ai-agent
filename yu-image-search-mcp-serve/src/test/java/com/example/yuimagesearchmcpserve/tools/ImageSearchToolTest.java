package com.example.yuimagesearchmcpserve.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
// ImageSearchToolTest,我名字改成ImageSearchTool就有问题了。woc
public class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
    void searchImage(){
        String result = imageSearchTool.searchImage("computer");
        Assertions.assertNotNull(result);
    }


}
