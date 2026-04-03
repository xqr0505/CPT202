package edu.xjtlu.cpt202.backend.modules.specialist.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.specialist.mapper.AdminSpecialistMapper;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.specialist.service.AdminSpecialistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
