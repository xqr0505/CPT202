package edu.xjtlu.cpt202.backend.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AvatarStorageService {

    String uploadUserAvatar(Long userId, MultipartFile file);
}
