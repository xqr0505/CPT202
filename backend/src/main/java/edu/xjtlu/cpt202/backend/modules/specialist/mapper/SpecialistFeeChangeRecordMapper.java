package edu.xjtlu.cpt202.backend.modules.specialist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.xjtlu.cpt202.backend.modules.specialist.model.entity.SpecialistFeeChangeRecord;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.SpecialistFeeChangeRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SpecialistFeeChangeRecordMapper extends BaseMapper<SpecialistFeeChangeRecord> {

    @Select("""
            SELECT
                id,
                specialist_id AS specialistId,
                old_fee AS oldFee,
                new_fee AS newFee,
                level,
                range_min AS rangeMin,
                range_max AS rangeMax,
                out_of_range AS outOfRange,
                changed_by_user_id AS changedByUserId,
                COALESCE(changed_by_name, '') AS changedByName,
                created_at AS createdAt
            FROM specialist_fee_change_records
            WHERE specialist_id = #{specialistId}
            ORDER BY created_at DESC, id DESC
            """)
    List<SpecialistFeeChangeRecordVO> selectBySpecialistId(@Param("specialistId") Long specialistId);
}
