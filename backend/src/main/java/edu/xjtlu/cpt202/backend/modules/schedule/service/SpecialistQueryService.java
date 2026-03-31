package edu.xjtlu.cpt202.backend.modules.schedule.service;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.SpecialistSearchQueryDTO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistCategoryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;

import java.time.LocalDate;
import java.util.List;

public interface SpecialistQueryService {

    List<SpecialistCategoryVO> listCategories();

    PageResult<SpecialistSummaryVO> searchSpecialists(SpecialistSearchQueryDTO query);

    SpecialistDetailVO getSpecialistDetail(Long specialistId);

    List<SpecialistAvailabilityVO> listAvailability(Long specialistId, LocalDate date);
}
