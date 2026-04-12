package edu.xjtlu.cpt202.backend.common.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.service.FileUploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/admin/uploads")
public class AdminUploadController {

    private final FileUploadService fileUploadService;

    public AdminUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/images")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileUploadService.uploadImage(file, "specialists");
        return Result.success(Map.of("url", url));
    }
}
