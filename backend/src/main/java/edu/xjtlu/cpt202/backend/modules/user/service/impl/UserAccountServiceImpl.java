package edu.xjtlu.cpt202.backend.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.xjtlu.cpt202.backend.common.enums.AccountStatusEnum;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.enums.UserRoleEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.storage.AvatarStorageService;
import edu.xjtlu.cpt202.backend.modules.auth.mapper.RefreshTokenMapper;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.auth.model.entity.RefreshToken;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangePasswordDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.UpdateUserProfileDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserAvatarUploadVO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserProfileVO;
import edu.xjtlu.cpt202.backend.modules.user.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_AVATAR_FILE_SIZE_BYTES = 2 * 1024 * 1024L;

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final AvatarStorageService avatarStorageService;

    @Override
    public UserProfileVO getCurrentUserProfile() {
        return toUserProfileVO(getCurrentUserOrThrow());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentUserProfile(UpdateUserProfileDTO request) {
        User user = getCurrentUserOrThrow();
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        ensureEmailUnique(normalizedEmail, user.getId());

        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(request.getPhoneNumber().trim());

        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to update profile");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAvatarUploadVO uploadCurrentUserAvatar(MultipartFile file) {
        validateAvatarFile(file);

        User user = getCurrentUserOrThrow();
        String avatarUrl = avatarStorageService.uploadUserAvatar(user.getId(), file);
        user.setAvatarUrl(avatarUrl);

        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to save avatar");
        }

        return new UserAvatarUploadVO(avatarUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO request) {
        User user = getCurrentUserOrThrow();

        if (!PASSWORD_ENCODER.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Confirmation password does not match");
        }

        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordHash(PASSWORD_ENCODER.encode(request.getNewPassword()));

        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to update password");
        }

        refreshTokenMapper.delete(new QueryWrapper<RefreshToken>().eq("user_id", user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateCurrentUserAccount() {
        User user = getCurrentUserOrThrow();

        if (AccountStatusEnum.DEACTIVATED.name().equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Account is already deactivated");
        }

        user.setStatus(AccountStatusEnum.DEACTIVATED.name());
        user.setLoginFailCount(0);
        user.setLockTime(null);

        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to deactivate account");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUser(String email, String rawPassword, String role, String fullName) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        String normalizedRole = role == null ? null : role.trim().toUpperCase(Locale.ROOT);

        if (!StringUtils.hasText(normalizedEmail)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Email is required");
        }
        if (!StringUtils.hasText(rawPassword)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Password is required");
        }
        if (!StringUtils.hasText(normalizedRole)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "User role is required");
        }
        if (!UserRoleEnum.ADMIN.name().equals(normalizedRole)
                && !UserRoleEnum.SPECIALIST.name().equals(normalizedRole)
                && !UserRoleEnum.CUSTOMER.name().equals(normalizedRole)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Invalid user role");
        }

        Long existingCount = userMapper.selectCount(new QueryWrapper<User>().eq("email", normalizedEmail));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode(), "This email is already registered");
        }

        User newUser = User.builder()
                .email(normalizedEmail)
                .passwordHash(PASSWORD_ENCODER.encode(rawPassword))
                .role(normalizedRole)
                .status(AccountStatusEnum.ACTIVE.name())
                .fullName(StringUtils.hasText(fullName) ? fullName.trim() : null)
                .loginFailCount(0)
                .lockTime(null)
                .build();
        userMapper.insert(newUser);
        return newUser;
    }

    private User getCurrentUserOrThrow() {
        Long currentUserId = getCurrentUserId();
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "User not found");
        }
        return user;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Long userId) {
            return userId;
        }

        if (principal instanceof Number number) {
            return number.longValue();
        }

        if (principal instanceof String principalText
                && !principalText.isBlank()
                && !"anonymousUser".equals(principalText)) {
            try {
                return Long.valueOf(principalText);
            } catch (NumberFormatException ignored) {
                // Fall through to unauthorized error below.
            }
        }

        throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
    }

    private void ensureEmailUnique(String email, Long currentUserId) {
        User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
                        .ne(User::getId, currentUserId)
                        .last("LIMIT 1")
        );

        if (existingUser != null) {
            throw new BusinessException(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode(), "This email is already registered");
        }
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please select an image to upload");
        }

        if (file.getSize() > MAX_AVATAR_FILE_SIZE_BYTES) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Avatar image must be 2 MB or smaller");
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String normalizedExtension = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);

        boolean hasAllowedExtension = ALLOWED_AVATAR_EXTENSIONS.contains(normalizedExtension);
        boolean hasAllowedContentType = contentType.isBlank() || ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType);

        if (!hasAllowedExtension || !hasAllowedContentType) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST.getCode(),
                    "Only JPG, JPEG, PNG, and WEBP images are allowed"
            );
        }
    }

    private UserProfileVO toUserProfileVO(User user) {
        UserProfileVO userProfileVO = new UserProfileVO();
        userProfileVO.setId(user.getId());
        userProfileVO.setFullName(user.getFullName());
        userProfileVO.setEmail(user.getEmail());
        userProfileVO.setPhoneNumber(user.getPhoneNumber());
        userProfileVO.setAvatarUrl(user.getAvatarUrl());
        userProfileVO.setStatus(user.getStatus());
        return userProfileVO;
    }
}
