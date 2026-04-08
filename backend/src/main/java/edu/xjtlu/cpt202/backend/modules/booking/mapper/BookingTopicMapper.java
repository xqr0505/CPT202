package edu.xjtlu.cpt202.backend.modules.booking.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookingTopicMapper {

    @Select("""
            SELECT topic_name
            FROM booking_topics
            WHERE is_active = 1
            ORDER BY sort_order ASC, id ASC
            """)
    List<String> listActiveTopicNames();

    @Select("""
            SELECT COUNT(1)
            FROM booking_topics
            WHERE topic_name = #{topicName}
              AND is_active = 1
            """)
    Long countActiveTopicByName(@Param("topicName") String topicName);
}
