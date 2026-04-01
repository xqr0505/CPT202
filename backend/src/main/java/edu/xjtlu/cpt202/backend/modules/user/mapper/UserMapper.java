package edu.xjtlu.cpt202.backend.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
