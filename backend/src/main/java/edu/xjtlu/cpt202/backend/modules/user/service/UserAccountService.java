package edu.xjtlu.cpt202.backend.modules.user.service;

import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangePasswordDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.UpdateUserProfileDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserAvatarUploadVO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserAccountService {

    UserProfileVO getCurrentUserProfile();

    void updateCurrentUserProfile(UpdateUserProfileDTO request);

    UserAvatarUploadVO uploadCurrentUserAvatar(MultipartFile file);

    void changePassword(ChangePasswordDTO request);

    void deactivateCurrentUserAccount();

    User createUser(String email, String rawPassword, String role, String fullName);
}
