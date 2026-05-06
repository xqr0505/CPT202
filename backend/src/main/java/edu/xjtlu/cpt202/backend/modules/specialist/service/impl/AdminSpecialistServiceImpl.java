package edu.xjtlu.cpt202.backend.modules.specialist.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.enums.AccountStatusEnum;
import edu.xjtlu.cpt202.backend.common.enums.SpecialistLevelEnum;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.enums.UserRoleEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.enums.CancelReasonEnum;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.specialist.mapper.AdminSpecialistMapper;
import edu.xjtlu.cpt202.backend.modules.specialist.mapper.SpecialistFeeChangeRecordMapper;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistCreateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistUpdateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.entity.SpecialistFeeChangeRecord;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.SpecialistFeeChangeRecordVO;
import edu.xjtlu.cpt202.backend.modules.specialist.service.AdminSpecialistService;
import edu.xjtlu.cpt202.backend.modules.user.mapper.SpecialistProfileMapper;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.SpecialistProfile;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import edu.xjtlu.cpt202.backend.modules.user.service.UserAccountService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AdminSpecialistServiceImpl implements AdminSpecialistService {

    private static final Logger log = LoggerFactory.getLogger(AdminSpecialistServiceImpl.class);
    private static final String DEFAULT_SPECIALIST_INITIAL_PASSWORD = "12345Expertlink";

    private final AdminSpecialistMapper adminSpecialistMapper;
    private final SpecialistFeeChangeRecordMapper specialistFeeChangeRecordMapper;
    private final BookingMapper bookingMapper;
    private final UserMapper userMapper;
    private final UserAccountService userAccountService;
    private final SpecialistProfileMapper specialistProfileMapper;
    private final JavaMailSender mailSender;
    private final Environment env;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<AdminSpecialistListVO> listSpecialists(AdminSpecialistListQueryDTO query) {
        long pageNo = query.getPageNo() == null ? 1 : query.getPageNo();
        long pageSize = query.getPageSize() == null ? 10 : query.getPageSize();

        Page<AdminSpecialistListVO> page = new Page<>(pageNo, pageSize);
        IPage<AdminSpecialistListVO> resultPage = adminSpecialistMapper.pageSpecialists(page, query);

        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSpecialist(AdminSpecialistCreateDTO request) {
        Long categoryCount = adminSpecialistMapper.selectCategoryCountById(request.getCategoryId());
        if (categoryCount == null || categoryCount == 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Category not found");
        }

        String normalizedName = request.getName().trim();
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String initialPassword = DEFAULT_SPECIALIST_INITIAL_PASSWORD;
        String normalizedLevel = request.getLevel().trim();
        String normalizedAvatarUrl = request.getAvatarUrl() == null ? null : request.getAvatarUrl().trim();
        String normalizedBio = normalizeOptionalBio(request.getBio());
        String mappedStatus = mapToDbStatus(request.getStatus());
        validateSpecialistLevel(normalizedLevel);
        validateFeeWithinRange(normalizedLevel, request.getConsultationFee());

        User user = userAccountService.createUser(
                normalizedEmail,
                initialPassword,
                UserRoleEnum.SPECIALIST.name(),
                normalizedName
        );
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(normalizedAvatarUrl);
            if (userMapper.updateById(user) == 0) {
                throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to initialize specialist avatar");
            }
        }

        SpecialistProfile specialistProfile = SpecialistProfile.builder()
                .userId(user.getId())
                .categoryId(request.getCategoryId())
                .level(normalizedLevel)
                .consultationFee(request.getConsultationFee())
                .avatarUrl(normalizedAvatarUrl)
                .bio(normalizedBio)
                .status(mappedStatus)
                .build();
        specialistProfileMapper.insert(specialistProfile);

        CompletableFuture.runAsync(() -> sendSpecialistRegistrationNotification(
                specialistProfile.getId(),
                normalizedName,
                normalizedEmail,
                initialPassword
        ));
    }

    @Override
    public AdminSpecialistDetailVO getSpecialistDetail(Long id) {
        AdminSpecialistDetailVO detail = adminSpecialistMapper.selectSpecialistDetailById(id);
        if (detail == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        return detail;
    }

    @Override
    public List<SpecialistFeeChangeRecordVO> listFeeChangeRecords(Long id) {
        AdminSpecialistDetailVO existing = adminSpecialistMapper.selectSpecialistDetailById(id);
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        return specialistFeeChangeRecordMapper.selectBySpecialistId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpecialist(Long id, AdminSpecialistUpdateDTO request) {
        AdminSpecialistDetailVO existing = adminSpecialistMapper.selectSpecialistDetailById(id);
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }

        Long categoryCount = adminSpecialistMapper.selectCategoryCountById(request.getCategoryId());
        if (categoryCount == null || categoryCount == 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Category not found");
        }

        Long userId = adminSpecialistMapper.selectUserIdBySpecialistId(id);
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }

        String normalizedName = request.getName().trim();
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        boolean resetPasswordToDefault = Boolean.TRUE.equals(request.getResetPasswordToDefault());
        String normalizedPassword = resetPasswordToDefault
                ? DEFAULT_SPECIALIST_INITIAL_PASSWORD
                : normalizeOptionalPassword(request.getPassword());
        String normalizedLevel = request.getLevel().trim();
        String normalizedAvatarUrl = request.getAvatarUrl() == null ? null : request.getAvatarUrl().trim();
        String normalizedBio = normalizeOptionalBio(request.getBio());
        String mappedStatus = mapToDbStatus(request.getStatus());
        validateSpecialistLevel(normalizedLevel);
        ensureSpecialistEmailUnique(normalizedEmail, userId);

        int updatedProfileRows = adminSpecialistMapper.updateSpecialistProfileById(
                id,
                request.getCategoryId(),
                normalizedLevel,
                request.getConsultationFee(),
                normalizedAvatarUrl,
                normalizedBio,
                mappedStatus
        );
        if (updatedProfileRows == 0) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }

        saveFeeChangeRecordIfNeeded(id, existing, normalizedLevel, request.getConsultationFee());
        String passwordHash = normalizedPassword == null ? null : passwordEncoder.encode(normalizedPassword);
        int updatedUserRows = adminSpecialistMapper.updateUserAccountById(
                userId,
                normalizedName,
                normalizedEmail,
                normalizedAvatarUrl,
                passwordHash
        );
        if (updatedUserRows == 0) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }

        if (resetPasswordToDefault) {
            CompletableFuture.runAsync(() -> sendSpecialistPasswordResetNotification(
                    id,
                    normalizedName,
                    normalizedEmail,
                    DEFAULT_SPECIALIST_INITIAL_PASSWORD
            ));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSpecialistStatus(Long id, String status) {
        AdminSpecialistDetailVO existing = adminSpecialistMapper.selectSpecialistDetailById(id);
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }

        String mappedStatus = mapToDbStatus(status);
        int updatedRows = adminSpecialistMapper.updateSpecialistStatusById(id, mappedStatus);
        if (updatedRows == 0) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }

        List<Booking> impactedBookings = List.of();
        if ("INACTIVE".equals(mappedStatus)) {
            impactedBookings = cancelAffectedBookings(id);
        }

        List<Booking> finalImpactedBookings = impactedBookings;
        CompletableFuture.runAsync(() -> {
            sendSpecialistStatusNotification(id, existing.getName(), status);
            if (!finalImpactedBookings.isEmpty()) {
                sendBookingChangeNotifications(existing.getName(), finalImpactedBookings);
            }
        });

        return impactedBookings.size();
    }

    private void validateSpecialistLevel(String level) {
        if (SpecialistLevelEnum.fromName(level) == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Invalid specialist level");
        }
    }

    private void validateFeeWithinRange(String level, BigDecimal fee) {
        SpecialistLevelEnum specialistLevel = SpecialistLevelEnum.fromName(level);
        if (specialistLevel == null || fee == null) {
            return;
        }
        if (fee.compareTo(specialistLevel.getMinFee()) < 0 || fee.compareTo(specialistLevel.getMaxFee()) > 0) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST.getCode(),
                    String.format(
                            "Consultation fee for %s must be between %s and %s",
                            level,
                            specialistLevel.getMinFee(),
                            specialistLevel.getMaxFee()
                    )
            );
        }
    }

    private void saveFeeChangeRecordIfNeeded(
            Long specialistId,
            AdminSpecialistDetailVO existing,
            String level,
            BigDecimal newFee
    ) {
        BigDecimal oldFee = existing.getConsultationFee();
        if (oldFee == null || newFee == null || oldFee.compareTo(newFee) == 0) {
            return;
        }

        SpecialistLevelEnum specialistLevel = SpecialistLevelEnum.fromName(level);
        if (specialistLevel == null) {
            return;
        }

        Long changedByUserId = null;
        String changedByName = null;
        try {
            changedByUserId = SecurityUtils.getCurrentUserId();
            User operator = userMapper.selectById(changedByUserId);
            changedByName = operator == null ? null : operator.getFullName();
        } catch (BusinessException ignored) {
            // Preserve the fee update even if operator identity is not available.
        }

        boolean outOfRange = newFee.compareTo(specialistLevel.getMinFee()) < 0
                || newFee.compareTo(specialistLevel.getMaxFee()) > 0;

        SpecialistFeeChangeRecord record = SpecialistFeeChangeRecord.builder()
                .specialistId(specialistId)
                .oldFee(oldFee)
                .newFee(newFee)
                .level(level)
                .rangeMin(specialistLevel.getMinFee())
                .rangeMax(specialistLevel.getMaxFee())
                .outOfRange(outOfRange)
                .changedByUserId(changedByUserId)
                .changedByName(changedByName)
                .build();
        specialistFeeChangeRecordMapper.insert(record);
    }

    private List<Booking> cancelAffectedBookings(Long specialistId) {
        List<Booking> impactedBookings = bookingMapper.selectList(
                Wrappers.<Booking>lambdaQuery()
                        .eq(Booking::getSpecialistId, specialistId)
                        .in(Booking::getStatus, BookingStatusEnum.PENDING.name(), BookingStatusEnum.CONFIRMED.name())
        );

        LocalDateTime now = LocalDateTime.now();
        for (Booking booking : impactedBookings) {
            booking.setStatus(BookingStatusEnum.CANCELLED.name());
            booking.setCancelledBy("ADMIN");
            booking.setCancelReason(CancelReasonEnum.BY_SPECIALIST_FORCE_MAJEURE.getDesc());
            booking.setDecisionTime(now);
            bookingMapper.updateById(booking);
        }
        return impactedBookings;
    }

    private String normalizeOptionalPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return null;
        }
        return password.trim();
    }

    private String normalizeOptionalBio(String bio) {
        if (!StringUtils.hasText(bio)) {
            return null;
        }
        return bio.trim();
    }

    private void ensureSpecialistEmailUnique(String normalizedEmail, Long currentUserId) {
        Long existingCount = userMapper.selectCount(
                new QueryWrapper<User>()
                        .eq("email", normalizedEmail)
                        .ne("id", currentUserId)
        );
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode(), "This email is already registered");
        }
    }

    private String mapToDbStatus(String status) {
        if ("Active".equals(status)) {
            return "ACTIVE";
        }
        if ("Inactive".equals(status)) {
            return "INACTIVE";
        }
        throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Invalid status");
    }

    private void sendSpecialistStatusNotification(Long specialistId, String specialistName, String status) {
        User specialistUser = adminSpecialistMapper.selectUserBySpecialistId(specialistId);
        if (specialistUser == null || specialistUser.getEmail() == null || specialistUser.getEmail().isBlank()) {
            log.warn("Skip specialist status notification: specialistId={} has no user email", specialistId);
            return;
        }

        String normalizedStatus = status == null ? "" : status.trim();
        String displayName = specialistName == null || specialistName.isBlank()
                ? specialistUser.getEmail()
                : specialistName;

        String subject;
        String content;
        if ("Inactive".equalsIgnoreCase(normalizedStatus)) {
            subject = "Your specialist account has been deactivated";
            content = String.format(
                    "Hello %s,%n%nYour specialist account has been deactivated by an administrator. You may be unable to use specialist features until the account is reactivated.%n%nIf you believe this was a mistake, please contact the platform administrator.",
                    displayName
            );
        } else if ("Active".equalsIgnoreCase(normalizedStatus)) {
            subject = "Your specialist account has been reactivated";
            content = String.format(
                    "Hello %s,%n%nYour specialist account has been reactivated by an administrator. You can now continue using specialist features on the platform.%n%nThank you.",
                    displayName
            );
        } else {
            log.warn("Skip specialist status notification: unsupported status={}, specialistId={}", status, specialistId);
            return;
        }

        log.info(
                "Preparing specialist status notification: specialistId={}, specialistName={}, email={}, newStatus={}",
                specialistId,
                displayName,
                specialistUser.getEmail(),
                normalizedStatus
        );

        try {
            sendEmail(specialistUser.getEmail(), subject, content, "specialist status notification", specialistId);

            log.info(
                "Specialist status notification sent successfully: specialistId={}, email={}, newStatus={}",
                specialistId,
                specialistUser.getEmail(),
                    normalizedStatus
            );
        } catch (Exception ex) {
            log.error(
                    "Failed to send specialist status notification: specialistId={}, email={}, newStatus={}, reason={}",
                    specialistId,
                    specialistUser.getEmail(),
                    normalizedStatus,
                    ex.getMessage(),
                    ex
            );
        }
    }

    private void sendSpecialistRegistrationNotification(
            Long specialistId,
            String specialistName,
            String specialistEmail,
            String initialPassword
    ) {
        if (!StringUtils.hasText(specialistEmail)) {
            log.warn("Skip specialist registration notification: specialistId={} has no email", specialistId);
            return;
        }

        String displayName = StringUtils.hasText(specialistName) ? specialistName : specialistEmail;
        String subject = "Your ExpertLink specialist account is ready";
        String content = String.format(
                "Hello %s,%n%n" +
                "Your specialist account has been created successfully by the administrator.%n%n" +
                "Login email: %s%n" +
                "Initial password: %s%n%n" +
                "Please log in and change your password as soon as possible.%n%n" +
                "ExpertLink Team",
                displayName,
                specialistEmail,
                initialPassword
        );

        sendEmail(
                specialistEmail,
                subject,
                content,
                "specialist registration notification",
                specialistId
        );
    }

    private void sendSpecialistPasswordResetNotification(
            Long specialistId,
            String specialistName,
            String specialistEmail,
            String resetPassword
    ) {
        if (!StringUtils.hasText(specialistEmail)) {
            log.warn("Skip specialist password reset notification: specialistId={} has no email", specialistId);
            return;
        }

        String displayName = StringUtils.hasText(specialistName) ? specialistName : specialistEmail;
        String subject = "Your ExpertLink password has been reset";
        String content = String.format(
                "Hello %s,%n%n" +
                "The administrator has reset your ExpertLink specialist password.%n%n" +
                "Login email: %s%n" +
                "Temporary password: %s%n%n" +
                "Please log in and change your password as soon as possible.%n%n" +
                "ExpertLink Team",
                displayName,
                specialistEmail,
                resetPassword
        );

        sendEmail(
                specialistEmail,
                subject,
                content,
                "specialist password reset notification",
                specialistId
        );
    }

    private void sendBookingChangeNotifications(String specialistName, List<Booking> impactedBookings) {
        for (Booking booking : impactedBookings) {
            User customer = userMapper.selectById(booking.getCustomerId());
            if (customer == null) {
                log.warn("Skip booking change notification: bookingId={} customer not found", booking.getId());
                continue;
            }
            if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                log.warn(
                        "Skip booking change notification: bookingId={} customerId={} has no email",
                        booking.getId(),
                        customer.getId()
                );
                continue;
            }

            String displayName = customer.getFullName() == null || customer.getFullName().isBlank()
                    ? customer.getEmail()
                    : customer.getFullName();
            String specialistDisplayName = specialistName == null || specialistName.isBlank()
                    ? "your specialist"
                    : specialistName;

            String subject = "Your booking was cancelled due to specialist deactivation";
            String content = String.format(
                    "Hello %s,%n%nYour booking (ID: %d) has been cancelled because specialist %s was deactivated by an administrator.%n%nPlease log in to ExpertLink and create a new booking with another available specialist.%n%nThank you for your understanding.",
                    displayName,
                    booking.getId(),
                    specialistDisplayName
            );

            sendEmail(
                    customer.getEmail(),
                    subject,
                    content,
                    "booking change notification",
                    booking.getId()
            );

            log.info(
                    "Booking change notification sent: bookingId={}, customerId={}, customerEmail={}, specialistName={}",
                    booking.getId(),
                    customer.getId(),
                    customer.getEmail(),
                    specialistDisplayName
            );
        }
    }

    private void sendEmail(String to, String subject, String content, String logContext, Long referenceId) {
        try {
            if (mailSender == null) {
                log.error("Mail sender not configured for {} referenceId={}", logContext, referenceId);
                return;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) {
                log.error("Mail sender returned null MimeMessage for {} referenceId={}", logContext, referenceId);
                return;
            }

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            String fromAddress = env == null ? null : env.getProperty("spring.mail.username");
            if (fromAddress == null || fromAddress.isBlank()) {
                fromAddress = "noreply@example.com";
            }

            helper.setFrom("ExpertLink <" + fromAddress + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content);
            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            log.error("Failed to send {} referenceId={}, reason={}", logContext, referenceId, ex.getMessage(), ex);
        }
    }
}
