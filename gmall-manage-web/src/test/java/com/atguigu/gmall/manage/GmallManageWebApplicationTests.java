package com.atguigu.gmall.manage;

import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageWebApplicationTests {


    @Test
    public void contextLoads() throws Exception {
        // 先连链接tracker
        String path = GmallManageWebApplicationTests.class.getClassLoader().getResource("tracker.conf").getPath();
        System.out.println(path);
        ClientGlobal.init(path);

        TrackerClient trackerClient = new TrackerClient();

        TrackerServer connection = trackerClient.getConnection();

        // 通过tracker获得storage
        StorageClient storageClient = new StorageClient(connection, null);

        String imgUrl = "Http://192.168.232.128";

        // 通过storage上传
        String[] jpgs = storageClient.upload_file("D:\\a.jpg", "jpg", null);

        for (String jpg : jpgs) {
            imgUrl += "/" + jpg;
        }
        System.out.println(imgUrl);

    }
    @Test
    public void test(){

    }


}


