package edu.xjtlu.cpt202.backend.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.xjtlu.cpt202.backend.modules.auth.model.entity.VerificationCode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VerificationCodeMapper extends BaseMapper<VerificationCode> {
}
