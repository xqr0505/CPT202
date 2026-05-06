package edu.xjtlu.cpt202.backend.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xjtlu.cpt202.backend.common.enums.AccountStatusEnum;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.enums.UserRoleEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.storage.AvatarStorageService;
import edu.xjtlu.cpt202.backend.modules.auth.model.entity.VerificationCode;
import edu.xjtlu.cpt202.backend.modules.auth.mapper.RefreshTokenMapper;
import edu.xjtlu.cpt202.backend.modules.auth.model.entity.RefreshToken;
import edu.xjtlu.cpt202.backend.modules.auth.service.VerificationCodeService;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangeCurrentUserEmailDTO;
import edu.xjtlu.cpt202.backend.modules.user.mapper.SpecialistProfileMapper;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserSecurityActivityMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangePasswordDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.SendChangeEmailCodeDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.UpdateUserProfileDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.SpecialistProfile;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.UserSecurityActivity;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserAvatarUploadVO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserProfileVO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserSecurityActivityVO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.VerifyPasswordVO;
import edu.xjtlu.cpt202.backend.modules.user.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private static final Logger logger = LoggerFactory.getLogger(UserAccountServiceImpl.class);
    private static final String CURRENT_PASSWORD_REQUIRED_MESSAGE = "Current password is required";
    private static final String CURRENT_PASSWORD_INCORRECT_MESSAGE = "Current password is incorrect";
    private static final String ACTIVITY_TYPE_PROFILE_UPDATED = "PROFILE_UPDATED";
    private static final String ACTIVITY_TYPE_EMAIL_CHANGED = "EMAIL_CHANGED";
    private static final String ACTIVITY_TYPE_PHONE_CHANGED = "PHONE_CHANGED";
    private static final String ACTIVITY_TYPE_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    private static final String ACTIVITY_TYPE_AVATAR_UPDATED = "AVATAR_UPDATED";
    private static final String ACTIVITY_TYPE_ACCOUNT_DEACTIVATED = "ACCOUNT_DEACTIVATED";
    private static final String EMAIL_CHANGE_REQUIRED_MESSAGE = "Use the email verification flow to change your email address.";
    private static final String CHANGE_EMAIL_VERIFICATION_TYPE = "CHANGE_EMAIL";
    private static final String CHANGE_EMAIL_EMAIL_SUBJECT = "Confirm your new email address";
    private static final String CHANGE_EMAIL_EMAIL_TEMPLATE =
            "Use this verification code to confirm your new email address: %s\nThis code will expire in %d minutes.\nIf you did not request this change, please ignore this email.";
    private static final int SECURITY_ACTIVITY_LIMIT = 8;
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_AVATAR_FILE_SIZE_BYTES = 2 * 1024 * 1024L;
    private static final String CHANGED_FIELD_FULL_NAME = "fullName";
    private static final String CHANGED_FIELD_EMAIL = "email";
    private static final String CHANGED_FIELD_PHONE_NUMBER = "phoneNumber";
    private static final String CHANGED_FIELD_AVATAR_URL = "avatarUrl";
    private static final String CHANGED_FIELD_PASSWORD = "password";
    private static final String CHANGED_FIELD_STATUS = "status";
    private static final String PHONE_NUMBER_PATTERN = "^\\+\\d{1,3}\\s\\d{4,14}$";

    private final UserMapper userMapper;
    private final SpecialistProfileMapper specialistProfileMapper;
    private final UserSecurityActivityMapper userSecurityActivityMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final AvatarStorageService avatarStorageService;
    private final VerificationCodeService verificationCodeService;
    private final ObjectMapper objectMapper;

    @Override
    public UserProfileVO getCurrentUserProfile() {
        User user = getCurrentUserOrThrow();
        return toUserProfileVO(user, resolveProfileAvatarUrl(user));
    }

    @Override
    public List<UserSecurityActivityVO> getCurrentUserSecurityActivities() {
        User user = getCurrentUserOrThrow();

        return userSecurityActivityMapper.selectList(
                        new LambdaQueryWrapper<UserSecurityActivity>()
                                .eq(UserSecurityActivity::getUserId, user.getId())
                                .orderByDesc(UserSecurityActivity::getCreatedAt)
                                .orderByDesc(UserSecurityActivity::getId)
                                .last("LIMIT " + SECURITY_ACTIVITY_LIMIT)
                ).stream()
                .map(this::toUserSecurityActivityVO)
                .collect(Collectors.toList());
    }

    @Override
    public VerifyPasswordVO verifyCurrentUserPassword(String currentPassword) {
        User user = getCurrentUserOrThrow();
        assertCurrentPasswordMatches(user, currentPassword);
        return new VerifyPasswordVO(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentUserProfile(UpdateUserProfileDTO request) {
        User user = getCurrentUserOrThrow();

        if (request.getEmail() != null
                && !normalizeEmail(request.getEmail()).equals(normalizeEmail(user.getEmail()))) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), EMAIL_CHANGE_REQUIRED_MESSAGE);
        }

        List<String> changedFields = new ArrayList<>();
        String nextFullName = normalizeNullableText(request.getFullName());
        String nextPhoneNumber = normalizeNullableText(request.getPhoneNumber());

        if (request.getPhoneNumber() != null && StringUtils.hasText(nextPhoneNumber) && !nextPhoneNumber.matches(PHONE_NUMBER_PATTERN)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please enter a valid phone number.");
        }

        if (request.getFullName() != null && !safeEquals(user.getFullName(), nextFullName)) {
            user.setFullName(nextFullName);
            changedFields.add(CHANGED_FIELD_FULL_NAME);
        }

        if (request.getPhoneNumber() != null && !safeEquals(user.getPhoneNumber(), nextPhoneNumber)) {
            user.setPhoneNumber(nextPhoneNumber);
            changedFields.add(CHANGED_FIELD_PHONE_NUMBER);
        }

        if (changedFields.isEmpty()) {
            return;
        }

        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to update profile");
        }

        recordSecurityActivitySafely(
                user.getId(),
                resolveProfileActivityType(changedFields),
                buildProfileActivityDescription(changedFields),
                changedFields
        );
    }

    @Override
    public void sendCurrentUserEmailChangeCode(SendChangeEmailCodeDTO request) {
        User user = getCurrentUserOrThrow();
        String normalizedNewEmail = normalizeEmail(request.getNewEmail());

        validateNewEmailForChange(user, normalizedNewEmail);
        verificationCodeService.sendCode(
                normalizedNewEmail,
                CHANGE_EMAIL_VERIFICATION_TYPE,
                CHANGE_EMAIL_EMAIL_SUBJECT,
                CHANGE_EMAIL_EMAIL_TEMPLATE
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO changeCurrentUserEmail(ChangeCurrentUserEmailDTO request) {
        User user = getCurrentUserOrThrow();
        String normalizedNewEmail = normalizeEmail(request.getNewEmail());

        assertCurrentPasswordMatches(user, request.getCurrentPassword());
        validateNewEmailForChange(user, normalizedNewEmail);

        VerificationCode codeRecord = verificationCodeService.requireLatestValidCode(
                normalizedNewEmail,
                CHANGE_EMAIL_VERIFICATION_TYPE,
                request.getCode(),
                "Invalid or expired verification code"
        );

        user.setEmail(normalizedNewEmail);
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to update email");
        }

        verificationCodeService.markCodeUsed(codeRecord);
        recordSecurityActivitySafely(
                user.getId(),
                ACTIVITY_TYPE_EMAIL_CHANGED,
                "Email changed",
                List.of(CHANGED_FIELD_EMAIL)
        );
        return toUserProfileVO(user, resolveProfileAvatarUrl(user));
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
        syncSpecialistAvatarIfNeeded(user, avatarUrl);

        recordSecurityActivitySafely(
                user.getId(),
                ACTIVITY_TYPE_AVATAR_UPDATED,
                "Avatar updated",
                List.of(CHANGED_FIELD_AVATAR_URL)
        );
        return new UserAvatarUploadVO(avatarUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO request) {
        User user = getCurrentUserOrThrow();

        assertCurrentPasswordMatches(user, request.getCurrentPassword());

        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Confirmation password does not match");
        }

        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordHash(PASSWORD_ENCODER.encode(request.getNewPassword()));

        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to update password");
        }

        revokeRefreshTokens(user.getId());
        recordSecurityActivitySafely(
                user.getId(),
                ACTIVITY_TYPE_PASSWORD_CHANGED,
                "Password changed",
                List.of(CHANGED_FIELD_PASSWORD)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateCurrentUserAccount(String currentPassword) {
        User user = getCurrentUserOrThrow();

        if (AccountStatusEnum.DEACTIVATED.name().equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Account is already deactivated");
        }

        assertCurrentPasswordMatches(user, currentPassword);
        user.setStatus(AccountStatusEnum.DEACTIVATED.name());
        user.setLoginFailCount(0);
        user.setLockTime(null);

        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to deactivate account");
        }

        revokeRefreshTokens(user.getId());
        recordSecurityActivitySafely(
                user.getId(),
                ACTIVITY_TYPE_ACCOUNT_DEACTIVATED,
                "Account deactivated",
                List.of(CHANGED_FIELD_STATUS)
        );
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

    private void validateNewEmailForChange(User user, String normalizedNewEmail) {
        if (!StringUtils.hasText(normalizedNewEmail)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "New email is required");
        }

        if (normalizedNewEmail.equals(normalizeEmail(user.getEmail()))) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST.getCode(),
                    "New email must be different from your current email"
            );
        }

        ensureEmailUnique(normalizedNewEmail, user.getId());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void assertCurrentPasswordMatches(User user, String currentPassword) {
        if (!StringUtils.hasText(currentPassword)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), CURRENT_PASSWORD_REQUIRED_MESSAGE);
        }

        if (!PASSWORD_ENCODER.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), CURRENT_PASSWORD_INCORRECT_MESSAGE);
        }
    }

    private void revokeRefreshTokens(Long userId) {
        refreshTokenMapper.delete(new QueryWrapper<RefreshToken>().eq("user_id", userId));
    }

    private void recordSecurityActivitySafely(
            Long userId,
            String activityType,
            String description,
            List<String> changedFields
    ) {
        try {
            userSecurityActivityMapper.insert(UserSecurityActivity.builder()
                    .userId(userId)
                    .activityType(activityType)
                    .description(description)
                    .changedFields(writeChangedFields(changedFields))
                    .build());
        } catch (Exception exception) {
            logger.warn(
                    "Failed to record security activity for user {} and event {}: {}",
                    userId,
                    activityType,
                    exception.getMessage(),
                    exception
            );
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

    private UserProfileVO toUserProfileVO(User user, String avatarUrl) {
        UserProfileVO userProfileVO = new UserProfileVO();
        userProfileVO.setId(user.getId());
        userProfileVO.setFullName(user.getFullName());
        userProfileVO.setEmail(user.getEmail());
        userProfileVO.setPhoneNumber(user.getPhoneNumber());
        userProfileVO.setAvatarUrl(avatarUrl);
        userProfileVO.setStatus(user.getStatus());
        return userProfileVO;
    }

    private String resolveProfileAvatarUrl(User user) {
        String fallbackAvatarUrl = normalizeNullableText(user.getAvatarUrl());
        if (!isSpecialistUser(user)) {
            return fallbackAvatarUrl;
        }

        Long specialistProfileId = specialistProfileMapper.selectIdByUserId(user.getId());
        if (specialistProfileId == null) {
            return fallbackAvatarUrl;
        }

        SpecialistProfile specialistProfile = specialistProfileMapper.selectById(specialistProfileId);
        if (specialistProfile == null) {
            return fallbackAvatarUrl;
        }

        String specialistAvatarUrl = normalizeNullableText(specialistProfile.getAvatarUrl());
        return StringUtils.hasText(specialistAvatarUrl) ? specialistAvatarUrl : fallbackAvatarUrl;
    }

    private void syncSpecialistAvatarIfNeeded(User user, String avatarUrl) {
        if (!isSpecialistUser(user)) {
            return;
        }

        Long specialistProfileId = specialistProfileMapper.selectIdByUserId(user.getId());
        if (specialistProfileId == null) {
            logger.warn("No specialist profile found when syncing avatar for specialist user {}", user.getId());
            return;
        }

        SpecialistProfile specialistProfile = specialistProfileMapper.selectById(specialistProfileId);
        if (specialistProfile == null) {
            logger.warn("Specialist profile {} not found when syncing avatar for user {}", specialistProfileId, user.getId());
            return;
        }

        specialistProfile.setAvatarUrl(normalizeNullableText(avatarUrl));
        if (specialistProfileMapper.updateById(specialistProfile) == 0) {
            logger.warn("Failed to sync avatar for specialist profile {} (user {})", specialistProfileId, user.getId());
        }
    }

    private boolean isSpecialistUser(User user) {
        return user != null && UserRoleEnum.SPECIALIST.name().equalsIgnoreCase(normalizeNullableText(user.getRole()));
    }

    private UserSecurityActivityVO toUserSecurityActivityVO(UserSecurityActivity activity) {
        UserSecurityActivityVO userSecurityActivityVO = new UserSecurityActivityVO();
        userSecurityActivityVO.setId(activity.getId());
        userSecurityActivityVO.setActivityType(activity.getActivityType());
        userSecurityActivityVO.setDescription(activity.getDescription());
        userSecurityActivityVO.setChangedFields(readChangedFields(activity.getChangedFields()));
        userSecurityActivityVO.setCreatedAt(activity.getCreatedAt());
        return userSecurityActivityVO;
    }

    private String resolveProfileActivityType(List<String> changedFields) {
        if (changedFields.size() == 1 && changedFields.contains(CHANGED_FIELD_PHONE_NUMBER)) {
            return ACTIVITY_TYPE_PHONE_CHANGED;
        }

        return ACTIVITY_TYPE_PROFILE_UPDATED;
    }

    private String buildProfileActivityDescription(List<String> changedFields) {
        if (changedFields.size() == 1 && changedFields.contains(CHANGED_FIELD_PHONE_NUMBER)) {
            return "Profile updated (phone number)";
        }

        if (changedFields.size() == 1 && changedFields.contains(CHANGED_FIELD_FULL_NAME)) {
            return "Profile updated (full name)";
        }

        return "Profile updated";
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private boolean safeEquals(String left, String right) {
        return normalizeNullableText(left) == null
                ? normalizeNullableText(right) == null
                : normalizeNullableText(left).equals(normalizeNullableText(right));
    }

    private String writeChangedFields(List<String> changedFields) {
        try {
            return objectMapper.writeValueAsString(changedFields == null ? Collections.emptyList() : changedFields);
        } catch (JsonProcessingException exception) {
            logger.warn("Failed to serialize changed fields: {}", exception.getMessage(), exception);
            return "[]";
        }
    }

    private List<String> readChangedFields(String changedFields) {
        if (!StringUtils.hasText(changedFields)) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(changedFields, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            logger.warn("Failed to parse changed fields JSON: {}", exception.getMessage(), exception);
            return Collections.emptyList();
        }
    }
}
