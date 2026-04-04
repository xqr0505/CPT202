package edu.xjtlu.cpt202.backend.modules.specialist.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.specialist.mapper.AdminSpecialistMapper;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistUpdateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.specialist.service.AdminSpecialistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSpecialistServiceImpl implements AdminSpecialistService {

    private final AdminSpecialistMapper adminSpecialistMapper;

    @Override
    public PageResult<AdminSpecialistListVO> listSpecialists(AdminSpecialistListQueryDTO query) {
        long pageNo = query.getPageNo() == null ? 1 : query.getPageNo();
        long pageSize = query.getPageSize() == null ? 10 : query.getPageSize();

        Page<AdminSpecialistListVO> page = new Page<>(pageNo, pageSize);
        IPage<AdminSpecialistListVO> resultPage = adminSpecialistMapper.pageSpecialists(page, query);

        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
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
        String normalizedLevel = request.getLevel().trim();
        String normalizedAvatarUrl = request.getAvatarUrl() == null ? null : request.getAvatarUrl().trim();
        String mappedStatus = mapToDbStatus(request.getStatus());

        int updatedProfileRows = adminSpecialistMapper.updateSpecialistProfileById(
                id,
                request.getCategoryId(),
                normalizedLevel,
                request.getConsultationFee(),
                normalizedAvatarUrl,
                mappedStatus
        );
        if (updatedProfileRows == 0) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }

        adminSpecialistMapper.updateUserFullNameById(userId, normalizedName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpecialistStatus(Long id, String status) {
        AdminSpecialistDetailVO existing = adminSpecialistMapper.selectSpecialistDetailById(id);
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }

        String mappedStatus = mapToDbStatus(status);
        int updatedRows = adminSpecialistMapper.updateSpecialistStatusById(id, mappedStatus);
        if (updatedRows == 0) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
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
}
