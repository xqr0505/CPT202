package edu.xjtlu.cpt202.backend.modules.user.service.impl;

import edu.xjtlu.cpt202.backend.common.enums.AccountStatusEnum;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangePasswordDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.UpdateUserProfileDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserProfileVO;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        assertEquals(AccountStatusEnum.ACTIVE.name(), profile.getStatus());

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
    void updateCurrentUserProfile_updatesAuthenticatedUserAndPersistsTrimmedFields() {
        authenticateAs(7L);
        User currentUser = buildUser();
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setFullName("  Alice Smith  ");
        request.setEmail("  ALICE.SMITH@EXAMPLE.COM  ");
        request.setPhoneNumber("  +86 13900139000  ");

        when(userMapper.selectById(7L)).thenReturn(currentUser);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> userAccountService.updateCurrentUserProfile(request));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).selectById(7L);
        verify(userMapper).selectOne(any());
        verify(userMapper).updateById(userCaptor.capture());
        verifyNoMoreInteractions(userMapper);

        User savedUser = userCaptor.getValue();
        assertEquals(7L, savedUser.getId());
        assertEquals("Alice Smith", savedUser.getFullName());
        assertEquals("alice.smith@example.com", savedUser.getEmail());
        assertEquals("+86 13900139000", savedUser.getPhoneNumber());
        assertEquals(AccountStatusEnum.ACTIVE.name(), savedUser.getStatus());
    }

    @Test
    void updateCurrentUserProfile_whenEmailBelongsToAnotherUser_throwsDuplicateEmail() {
        authenticateAs(7L);
        User currentUser = buildUser();
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setFullName("Alice Smith");
        request.setEmail("used@example.com");
        request.setPhoneNumber("+86 13900139000");

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
        verifyNoMoreInteractions(userMapper);
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
        verifyNoMoreInteractions(userMapper);

        User savedUser = userCaptor.getValue();
        assertNotEquals("NewPass456", savedUser.getPasswordHash());
        assertNotEquals(oldPasswordHash, savedUser.getPasswordHash());
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

        userAccountService.deactivateCurrentUserAccount();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).selectById(7L);
        verify(userMapper).updateById(userCaptor.capture());
        verifyNoMoreInteractions(userMapper);

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
                () -> userAccountService.deactivateCurrentUserAccount()
        );

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals("Account is already deactivated", exception.getMessage());

        verify(userMapper).selectById(7L);
        verify(userMapper, never()).updateById(any(User.class));
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
                .status(AccountStatusEnum.ACTIVE.name())
                .role("CUSTOMER")
                .passwordHash(PASSWORD_ENCODER.encode("OldPass123"))
                .build();
    }
}
