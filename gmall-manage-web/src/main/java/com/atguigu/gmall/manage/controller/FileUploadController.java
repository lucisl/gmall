package com.atguigu.gmall.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@CrossOrigin
@RestController
public class FileUploadController {
    @Value("${fileServer.url}")
    private String FileUrl;

    /**
     * 文件上传
     *
     * @param file
     * @return
     * @throws IOException
     * @throws MyException
     */
    @RequestMapping(value = "/fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {
        String imgUrl = FileUrl;
        if (file != null) {
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer = trackerClient.getConnection();
            StorageClient storageClient = new StorageClient(trackerServer, null);
            //获取文件名称
            //String orginalFilename = "D://IMG_0594.JPG";
            String orginalFilename = file.getOriginalFilename();
            String suffix = StringUtils.substringAfterLast(orginalFilename, ".");

            String[] upload_file = storageClient.upload_file(file.getBytes(), suffix, null);

            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
                imgUrl += "/" + path;
            }
        }
        return imgUrl;

    }
}
