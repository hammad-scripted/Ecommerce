package com.ecommerce.project.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService{
    @Override
    public String uploadImage(String path, MultipartFile image) throws IOException {
        //** filename of current/original file
        String originalFileName=image.getOriginalFilename();

        //* generate a unique file name
        String randomId= UUID.randomUUID().toString();
//        ? mat.jpg---1234--1234.jpg
        String fileName=randomId.concat(originalFileName.substring(originalFileName.lastIndexOf(".")));
        String filePath=path+ File.pathSeparator+fileName;

        //* check if path exist and create
        File folder=new File(path);
        if(!folder.exists()){
            folder.mkdir();
        }
        Files.copy(image.getInputStream(), Paths.get(filePath));

        //** upload to server
        return fileName;
    }

}
