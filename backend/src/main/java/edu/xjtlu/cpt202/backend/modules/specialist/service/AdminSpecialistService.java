package edu.xjtlu.cpt202.backend.modules.specialist.service;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistUpdateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;

public interface AdminSpecialistService {

    PageResult<AdminSpecialistListVO> listSpecialists(AdminSpecialistListQueryDTO query);

    void createSpecialist(AdminSpecialistUpdateDTO request);

    AdminSpecialistDetailVO getSpecialistDetail(Long id);

    void updateSpecialist(Long id, AdminSpecialistUpdateDTO request);

    void updateSpecialistStatus(Long id, String status);
}
