package edu.xjtlu.cpt202.backend.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangePasswordDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.UpdateUserProfileDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
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

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final UserMapper userMapper;

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
    public void changePassword(ChangePasswordDTO request) {
        User user = getCurrentUserOrThrow();

        if (!PASSWORD_ENCODER.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Confirmation password does not match");
        }

        user.setPasswordHash(PASSWORD_ENCODER.encode(request.getNewPassword()));

        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to update password");
        }
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

    private UserProfileVO toUserProfileVO(User user) {
        UserProfileVO userProfileVO = new UserProfileVO();
        userProfileVO.setId(user.getId());
        userProfileVO.setFullName(user.getFullName());
        userProfileVO.setEmail(user.getEmail());
        userProfileVO.setPhoneNumber(user.getPhoneNumber());
        return userProfileVO;
    }
}
