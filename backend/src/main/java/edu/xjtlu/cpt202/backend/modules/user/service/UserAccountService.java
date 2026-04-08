package edu.xjtlu.cpt202.backend.modules.user.service;

import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangePasswordDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.UpdateUserProfileDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserProfileVO;

public interface UserAccountService {

    UserProfileVO getCurrentUserProfile();

    void updateCurrentUserProfile(UpdateUserProfileDTO request);

    void changePassword(ChangePasswordDTO request);

    void deactivateCurrentUserAccount();
}
