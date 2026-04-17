package edu.xjtlu.cpt202.backend.modules.user.service.impl;

import edu.xjtlu.cpt202.backend.common.enums.AccountStatusEnum;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.storage.AvatarStorageService;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserSecurityActivityMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangePasswordDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.UpdateUserProfileDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.UserSecurityActivity;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserAvatarUploadVO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserProfileVO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserSecurityActivityVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceImplTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserSecurityActivityMapper userSecurityActivityMapper;

    @Mock
    private AvatarStorageService avatarStorageService;

    @InjectMocks
    private UserAccountServiceImpl userAccountService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserProfile_returnsAuthenticatedUserProfile() {
        authenticateAs(7L);
        User currentUser = buildUser();
        when(userMapper.selectById(7L)).thenReturn(currentUser);

        UserProfileVO profile = userAccountService.getCurrentUserProfile();

        assertEquals(7L, profile.getId());
        assertEquals("Alice Johnson", profile.getFullName());
        assertEquals("alice@example.com", profile.getEmail());
        assertEquals("+86 13800138000", profile.getPhoneNumber());
        assertEquals("https://cdn.example.com/avatars/alice.jpg", profile.getAvatarUrl());
        assertEquals(AccountStatusEnum.ACTIVE.name(), profile.getStatus());

        verify(userMapper).selectById(7L);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void getCurrentUserProfile_whenAvatarMissing_returnsNullAvatarUrl() {
        authenticateAs(7L);
        User currentUser = buildUser();
        currentUser.setAvatarUrl(null);
        when(userMapper.selectById(7L)).thenReturn(currentUser);

        UserProfileVO profile = userAccountService.getCurrentUserProfile();

        assertNull(profile.getAvatarUrl());
        verify(userMapper).selectById(7L);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void getCurrentUserProfile_whenUserDoesNotExist_throwsNotFound() {
        authenticateAs(7L);
        when(userMapper.selectById(7L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.getCurrentUserProfile()
        );

        assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), exception.getCode());
        assertEquals("User not found", exception.getMessage());

        verify(userMapper).selectById(7L);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void getCurrentUserProfile_whenUnauthenticated_throwsUnauthorized() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.getCurrentUserProfile()
        );

        assertEquals(ResultCodeEnum.UNAUTHORIZED.getCode(), exception.getCode());
        verifyNoInteractions(userMapper);
    }

    @Test
    void getCurrentUserSecurityActivity_returnsRecentActivityEntries() {
        authenticateAs(7L);
        User currentUser = buildUser();
        LocalDateTime createdAt = LocalDateTime.now();
        UserSecurityActivity activity = UserSecurityActivity.builder()
                .id(11L)
                .userId(7L)
                .eventType("PASSWORD_CHANGED")
                .summary("Changed account password.")
                .createdAt(createdAt)
                .build();

        when(userMapper.selectById(7L)).thenReturn(currentUser);
        when(userSecurityActivityMapper.selectList(any())).thenReturn(List.of(activity));

        List<UserSecurityActivityVO> activities = userAccountService.getCurrentUserSecurityActivity();

        assertEquals(1, activities.size());
        assertEquals(11L, activities.get(0).getId());
        assertEquals("PASSWORD_CHANGED", activities.get(0).getEventType());
        assertEquals("Changed account password.", activities.get(0).getSummary());
        assertEquals(createdAt, activities.get(0).getCreatedAt());

        verify(userMapper).selectById(7L);
        verify(userSecurityActivityMapper).selectList(any());
        verifyNoMoreInteractions(userMapper, userSecurityActivityMapper);
    }

    @Test
    void updateCurrentUserProfile_updatesAuthenticatedUserAndPersistsTrimmedFields() {
        authenticateAs(7L);
        User currentUser = buildUser();
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setFullName("  Alice Smith  ");
        request.setEmail("  ALICE.SMITH@EXAMPLE.COM  ");
        request.setPhoneNumber("  +86 13900139000  ");
        request.setCurrentPassword("OldPass123");

        when(userMapper.selectById(7L)).thenReturn(currentUser);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> userAccountService.updateCurrentUserProfile(request));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).selectById(7L);
        verify(userMapper).selectOne(any());
        verify(userMapper).updateById(userCaptor.capture());
        verify(userSecurityActivityMapper).insert(any(UserSecurityActivity.class));
        verifyNoMoreInteractions(userMapper, userSecurityActivityMapper);

        User savedUser = userCaptor.getValue();
        assertEquals(7L, savedUser.getId());
        assertEquals("Alice Smith", savedUser.getFullName());
        assertEquals("alice.smith@example.com", savedUser.getEmail());
        assertEquals("+86 13900139000", savedUser.getPhoneNumber());
        assertEquals(AccountStatusEnum.ACTIVE.name(), savedUser.getStatus());
    }

    @Test
    void updateCurrentUserProfile_whenEmailUnchanged_doesNotRequireCurrentPassword() {
        authenticateAs(7L);
        User currentUser = buildUser();
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setFullName("Alice Johnson Updated");
        request.setEmail("  ALICE@EXAMPLE.COM ");
        request.setPhoneNumber("+86 13900139000");

        when(userMapper.selectById(7L)).thenReturn(currentUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> userAccountService.updateCurrentUserProfile(request));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).selectById(7L);
        verify(userMapper, never()).selectOne(any());
        verify(userMapper).updateById(userCaptor.capture());
        verify(userSecurityActivityMapper).insert(any(UserSecurityActivity.class));
        verifyNoMoreInteractions(userMapper, userSecurityActivityMapper);

        User savedUser = userCaptor.getValue();
        assertEquals("Alice Johnson Updated", savedUser.getFullName());
        assertEquals("alice@example.com", savedUser.getEmail());
        assertEquals("+86 13900139000", savedUser.getPhoneNumber());
    }

    @Test
    void updateCurrentUserProfile_whenEmailChangesWithoutCurrentPassword_throwsBadRequest() {
        authenticateAs(7L);
        User currentUser = buildUser();
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setFullName("Alice Smith");
        request.setEmail("alice.smith@example.com");
        request.setPhoneNumber("+86 13900139000");

        when(userMapper.selectById(7L)).thenReturn(currentUser);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.updateCurrentUserProfile(request)
        );

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals("Current password is required", exception.getMessage());

        verify(userMapper).selectById(7L);
        verify(userMapper, never()).selectOne(any());
        verify(userMapper, never()).updateById(any(User.class));
        verifyNoInteractions(userSecurityActivityMapper);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void updateCurrentUserProfile_whenEmailChangesWithWrongCurrentPassword_throwsBadRequest() {
        authenticateAs(7L);
        User currentUser = buildUser();
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setFullName("Alice Smith");
        request.setEmail("alice.smith@example.com");
        request.setPhoneNumber("+86 13900139000");
        request.setCurrentPassword("WrongPass123");

        when(userMapper.selectById(7L)).thenReturn(currentUser);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.updateCurrentUserProfile(request)
        );

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals("Current password is incorrect", exception.getMessage());

        verify(userMapper).selectById(7L);
        verify(userMapper, never()).selectOne(any());
        verify(userMapper, never()).updateById(any(User.class));
        verifyNoInteractions(userSecurityActivityMapper);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void updateCurrentUserProfile_whenEmailBelongsToAnotherUser_throwsDuplicateEmail() {
        authenticateAs(7L);
        User currentUser = buildUser();
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setFullName("Alice Smith");
        request.setEmail("used@example.com");
        request.setPhoneNumber("+86 13900139000");
        request.setCurrentPassword("OldPass123");

        when(userMapper.selectById(7L)).thenReturn(currentUser);
        when(userMapper.selectOne(any())).thenReturn(User.builder().id(99L).email("used@example.com").build());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.updateCurrentUserProfile(request)
        );

        assertEquals(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode(), exception.getCode());
        assertEquals("This email is already registered", exception.getMessage());

        verify(userMapper).selectById(7L);
        verify(userMapper).selectOne(any());
        verify(userMapper, never()).updateById(any(User.class));
        verifyNoInteractions(userSecurityActivityMapper);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void updateCurrentUserProfile_whenActivityLoggingFails_stillCompletesMainAction() {
        authenticateAs(7L);
        User currentUser = buildUser();
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setFullName("Alice Smith");
        request.setEmail("alice@example.com");
        request.setPhoneNumber("+86 13900139000");

        when(userMapper.selectById(7L)).thenReturn(currentUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(userSecurityActivityMapper.insert(any(UserSecurityActivity.class))).thenThrow(new RuntimeException("logging failed"));

        assertDoesNotThrow(() -> userAccountService.updateCurrentUserProfile(request));

        verify(userMapper).selectById(7L);
        verify(userMapper).updateById(any(User.class));
        verify(userSecurityActivityMapper).insert(any(UserSecurityActivity.class));
        verifyNoMoreInteractions(userMapper, userSecurityActivityMapper);
    }

    @Test
    void uploadCurrentUserAvatar_persistsUploadedAvatarUrl() {
        authenticateAs(7L);
        User currentUser = buildUser();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "avatar-bytes".getBytes()
        );

        when(userMapper.selectById(7L)).thenReturn(currentUser);
        when(avatarStorageService.uploadUserAvatar(7L, file))
                .thenReturn("https://cdn.example.com/avatars/new-avatar.png");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserAvatarUploadVO response = userAccountService.uploadCurrentUserAvatar(file);

        assertEquals("https://cdn.example.com/avatars/new-avatar.png", response.getAvatarUrl());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).selectById(7L);
        verify(avatarStorageService).uploadUserAvatar(7L, file);
        verify(userMapper).updateById(userCaptor.capture());
        verify(userSecurityActivityMapper).insert(any(UserSecurityActivity.class));
        verifyNoMoreInteractions(userMapper, userSecurityActivityMapper, avatarStorageService);

        assertEquals("https://cdn.example.com/avatars/new-avatar.png", userCaptor.getValue().getAvatarUrl());
    }

    @Test
    void uploadCurrentUserAvatar_whenFileTypeIsInvalid_throwsBadRequest() {
        authenticateAs(7L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.gif",
                "image/gif",
                "gif-bytes".getBytes()
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.uploadCurrentUserAvatar(file)
        );

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals("Only JPG, JPEG, PNG, and WEBP images are allowed", exception.getMessage());
        verifyNoInteractions(userMapper, userSecurityActivityMapper, avatarStorageService);
    }

    @Test
    void uploadCurrentUserAvatar_whenFileIsTooLarge_throwsBadRequest() {
        authenticateAs(7L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[2 * 1024 * 1024 + 1]
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.uploadCurrentUserAvatar(file)
        );

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals("Avatar image must be 2 MB or smaller", exception.getMessage());
        verifyNoInteractions(userMapper, userSecurityActivityMapper, avatarStorageService);
    }

    @Test
    void uploadCurrentUserAvatar_whenUnauthenticated_throwsUnauthorized() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "avatar-bytes".getBytes()
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.uploadCurrentUserAvatar(file)
        );

        assertEquals(ResultCodeEnum.UNAUTHORIZED.getCode(), exception.getCode());
        verifyNoInteractions(userMapper, userSecurityActivityMapper, avatarStorageService);
    }

    @Test
    void changePassword_updatesEncodedPasswordWhenCurrentPasswordMatches() {
        authenticateAs(7L);
        User currentUser = buildUser();
        currentUser.setPasswordHash(PASSWORD_ENCODER.encode("OldPass123"));
        String oldPasswordHash = currentUser.getPasswordHash();

        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setCurrentPassword("OldPass123");
        request.setNewPassword("NewPass456");
        request.setConfirmationPassword("NewPass456");

        when(userMapper.selectById(7L)).thenReturn(currentUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        userAccountService.changePassword(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).selectById(7L);
        verify(userMapper).updateById(userCaptor.capture());
        verify(userSecurityActivityMapper).insert(any(UserSecurityActivity.class));
        verifyNoMoreInteractions(userMapper, userSecurityActivityMapper);

        User savedUser = userCaptor.getValue();
        assertNotEquals("NewPass456", savedUser.getPasswordHash());
        assertNotEquals(oldPasswordHash, savedUser.getPasswordHash());
        assertNotNull(savedUser.getPasswordChangedAt());
        assertTrue(PASSWORD_ENCODER.matches("NewPass456", savedUser.getPasswordHash()));
    }

    @Test
    void changePassword_whenCurrentPasswordIsWrong_throwsBadRequest() {
        authenticateAs(7L);
        User currentUser = buildUser();
        currentUser.setPasswordHash(PASSWORD_ENCODER.encode("OldPass123"));

        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setCurrentPassword("WrongPass123");
        request.setNewPassword("NewPass456");
        request.setConfirmationPassword("NewPass456");

        when(userMapper.selectById(7L)).thenReturn(currentUser);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.changePassword(request)
        );

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals("Current password is incorrect", exception.getMessage());

        verify(userMapper).selectById(7L);
        verify(userMapper, never()).updateById(any(User.class));
        verifyNoInteractions(userSecurityActivityMapper);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void changePassword_whenConfirmationDoesNotMatch_throwsBadRequest() {
        authenticateAs(7L);
        User currentUser = buildUser();
        currentUser.setPasswordHash(PASSWORD_ENCODER.encode("OldPass123"));

        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setCurrentPassword("OldPass123");
        request.setNewPassword("NewPass456");
        request.setConfirmationPassword("Mismatch456");

        when(userMapper.selectById(7L)).thenReturn(currentUser);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.changePassword(request)
        );

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals("Confirmation password does not match", exception.getMessage());

        verify(userMapper).selectById(7L);
        verify(userMapper, never()).updateById(any(User.class));
        verifyNoInteractions(userSecurityActivityMapper);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void deactivateCurrentUserAccount_marksAccountAsDeactivatedWithoutDeletingRecord() {
        authenticateAs(7L);
        User currentUser = buildUser();
        currentUser.setLoginFailCount(3);
        currentUser.setLockTime(LocalDateTime.now().minusMinutes(5));

        when(userMapper.selectById(7L)).thenReturn(currentUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        userAccountService.deactivateCurrentUserAccount("OldPass123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).selectById(7L);
        verify(userMapper).updateById(userCaptor.capture());
        verify(userSecurityActivityMapper).insert(any(UserSecurityActivity.class));
        verifyNoMoreInteractions(userMapper, userSecurityActivityMapper);

        User savedUser = userCaptor.getValue();
        assertEquals(AccountStatusEnum.DEACTIVATED.name(), savedUser.getStatus());
        assertEquals(0, savedUser.getLoginFailCount());
        assertNull(savedUser.getLockTime());
        assertNull(savedUser.getDeletedAt());
    }

    @Test
    void deactivateCurrentUserAccount_whenAlreadyDeactivated_throwsBadRequest() {
        authenticateAs(7L);
        User currentUser = buildUser();
        currentUser.setStatus(AccountStatusEnum.DEACTIVATED.name());

        when(userMapper.selectById(7L)).thenReturn(currentUser);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.deactivateCurrentUserAccount("OldPass123")
        );

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals("Account is already deactivated", exception.getMessage());

        verify(userMapper).selectById(7L);
        verify(userMapper, never()).updateById(any(User.class));
        verifyNoInteractions(userSecurityActivityMapper);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void deactivateCurrentUserAccount_whenCurrentPasswordIsWrong_throwsBadRequest() {
        authenticateAs(7L);
        User currentUser = buildUser();

        when(userMapper.selectById(7L)).thenReturn(currentUser);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.deactivateCurrentUserAccount("WrongPass123")
        );

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals("Current password is incorrect", exception.getMessage());

        verify(userMapper).selectById(7L);
        verify(userMapper, never()).updateById(any(User.class));
        verifyNoInteractions(userSecurityActivityMapper);
        verifyNoMoreInteractions(userMapper);
    }

    private void authenticateAs(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, AuthorityUtils.NO_AUTHORITIES)
        );
    }

    private User buildUser() {
        return User.builder()
                .id(7L)
                .fullName("Alice Johnson")
                .email("alice@example.com")
                .phoneNumber("+86 13800138000")
                .avatarUrl("https://cdn.example.com/avatars/alice.jpg")
                .status(AccountStatusEnum.ACTIVE.name())
                .role("CUSTOMER")
                .passwordHash(PASSWORD_ENCODER.encode("OldPass123"))
                .passwordChangedAt(null)
                .build();
    }
}
