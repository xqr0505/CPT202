package edu.xjtlu.cpt202.backend.modules.user.service;

import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangePasswordDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangeCurrentUserEmailDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.SendChangeEmailCodeDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.UpdateUserProfileDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserAvatarUploadVO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserProfileVO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserSecurityActivityVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserAccountService {

    UserProfileVO getCurrentUserProfile();

    List<UserSecurityActivityVO> getCurrentUserSecurityActivity();

    void updateCurrentUserProfile(UpdateUserProfileDTO request);

    void sendCurrentUserEmailChangeCode(SendChangeEmailCodeDTO request);

    UserProfileVO changeCurrentUserEmail(ChangeCurrentUserEmailDTO request);

    UserAvatarUploadVO uploadCurrentUserAvatar(MultipartFile file);

    void changePassword(ChangePasswordDTO request);

    void deactivateCurrentUserAccount(String currentPassword);

    User createUser(String email, String rawPassword, String role, String fullName);
}
