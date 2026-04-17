package edu.xjtlu.cpt202.backend.modules.user.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangePasswordDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.ConfirmCurrentPasswordDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.UpdateUserProfileDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserAvatarUploadVO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserProfileVO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserSecurityActivityVO;
import edu.xjtlu.cpt202.backend.modules.user.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile() {
        return Result.success(userAccountService.getCurrentUserProfile());
    }

    @GetMapping("/security-activity")
    public Result<List<UserSecurityActivityVO>> getSecurityActivity() {
        return Result.success(userAccountService.getCurrentUserSecurityActivity());
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateUserProfileDTO request) {
        userAccountService.updateCurrentUserProfile(request);
        return Result.success();
    }

    @PostMapping("/avatar")
    public Result<UserAvatarUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userAccountService.uploadCurrentUserAvatar(file));
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO request) {
        userAccountService.changePassword(request);
        return Result.success();
    }

    @PostMapping("/deactivate")
    public Result<Void> deactivateCurrentUserAccount(@Valid @RequestBody ConfirmCurrentPasswordDTO request) {
        userAccountService.deactivateCurrentUserAccount(request.getCurrentPassword());
        return Result.success();
    }
}
