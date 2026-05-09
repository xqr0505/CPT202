package edu.xjtlu.cpt202.backend.modules.specialist.service;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistCreateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistUpdateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.SpecialistFeeChangeRecordVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminSpecialistService {

    PageResult<AdminSpecialistListVO> listSpecialists(AdminSpecialistListQueryDTO query);

    void createSpecialist(AdminSpecialistCreateDTO request);

    AdminSpecialistDetailVO getSpecialistDetail(Long id);

    List<SpecialistFeeChangeRecordVO> listFeeChangeRecords(Long id);

    void updateSpecialist(Long id, AdminSpecialistUpdateDTO request);

    int updateSpecialistStatus(Long id, String status);

    String uploadSpecialistAvatar(MultipartFile file);
}
