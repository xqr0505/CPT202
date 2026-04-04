package edu.xjtlu.cpt202.backend.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.SpecialistProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SpecialistProfileMapper extends BaseMapper<SpecialistProfile> {

    @Select("""
            SELECT id
            FROM specialist_profiles
            WHERE user_id = #{userId}
            LIMIT 1
            """)
    Long selectIdByUserId(@Param("userId") Long userId);
}
