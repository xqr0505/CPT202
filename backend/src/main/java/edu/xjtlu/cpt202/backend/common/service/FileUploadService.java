package edu.xjtlu.cpt202.backend.common.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    String uploadImage(MultipartFile file, String folder);
}
