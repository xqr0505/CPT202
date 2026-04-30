package edu.xjtlu.cpt202.backend.modules.schedule.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.SpecialistSearchQueryDTO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistCategoryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface SpecialistQueryMapper {

    @Select("""
            SELECT
                id,
                category_name AS name
            FROM expertise_categories
            ORDER BY category_name ASC
            """)
    List<SpecialistCategoryVO> listCategories();

    @Select("""
            <script>
            SELECT
                sp.id AS id,
                sp.user_id AS userId,
                COALESCE(NULLIF(u.full_name, ''), u.email) AS name,
                COALESCE(sp.avatar_url, '') AS avatarUrl,
                sp.category_id AS categoryId,
                COALESCE(c.category_name, '') AS categoryName,
                COALESCE(sp.level, '') AS level,
                sp.consultation_fee AS consultationFee,
                COALESCE(sp.bio, '') AS bio,
                COALESCE(sp.status, '') AS status,
                CASE
                    WHEN #{query.date} IS NULL THEN FALSE
                    WHEN sp.status != 'ACTIVE' THEN FALSE
                    WHEN EXISTS (
                        SELECT 1
                        FROM time_slots ts
                        WHERE ts.specialist_id = sp.id
                          AND ts.slot_date = #{query.date}
                          AND ts.status = 'AVAILABLE'
                          AND (
                              ts.slot_date > #{today}
                              OR (ts.slot_date = #{today} AND ts.start_time > #{currentTime})
                          )
                    ) THEN TRUE
                    ELSE FALSE
                END AS hasAvailabilityOnSelectedDate
            FROM specialist_profiles sp
            INNER JOIN users u ON u.id = sp.user_id
            LEFT JOIN expertise_categories c ON c.id = sp.category_id
            <where>
                <if test="query.keyword == null or query.keyword == ''">
                    sp.status = 'ACTIVE'
                </if>
                <if test="query.keyword != null and query.keyword != ''">
                    AND (
                        u.full_name LIKE CONCAT('%', #{query.keyword}, '%')
                        OR u.email LIKE CONCAT('%', #{query.keyword}, '%')
                    )
                </if>
                <if test="query.categoryId != null">
                    AND sp.category_id = #{query.categoryId}
                </if>
                <if test="query.date != null and (query.keyword == null or query.keyword == '')">
                    AND sp.status = 'ACTIVE'
                    AND EXISTS (
                        SELECT 1
                        FROM time_slots ts_filter
                        WHERE ts_filter.specialist_id = sp.id
                          AND ts_filter.slot_date = #{query.date}
                          AND ts_filter.status = 'AVAILABLE'
                          AND (
                              ts_filter.slot_date > #{today}
                              OR (ts_filter.slot_date = #{today} AND ts_filter.start_time > #{currentTime})
                          )
                    )
                </if>
            </where>
            <choose>
                <when test="query.sortBy == 'feeAsc'">
                    ORDER BY
                        CASE WHEN sp.status = 'ACTIVE' THEN 1 ELSE 0 END DESC,
                        sp.consultation_fee ASC,
                        sp.id DESC
                </when>
                <when test="query.sortBy == 'feeDesc'">
                    ORDER BY
                        CASE WHEN sp.status = 'ACTIVE' THEN 1 ELSE 0 END DESC,
                        sp.consultation_fee DESC,
                        sp.id DESC
                </when>
                <when test="query.sortBy == 'levelDesc'">
                    ORDER BY
                        CASE WHEN sp.status = 'ACTIVE' THEN 1 ELSE 0 END DESC,
                        CASE sp.level
                            WHEN 'CHIEF' THEN 4
                            WHEN 'SENIOR' THEN 3
                            WHEN 'INTERMEDIATE' THEN 2
                            WHEN 'JUNIOR' THEN 1
                            ELSE 0
                        END DESC,
                        sp.consultation_fee ASC,
                        sp.id DESC
                </when>
                <otherwise>
                    ORDER BY
                        CASE WHEN sp.status = 'ACTIVE' THEN 1 ELSE 0 END DESC,
                        CASE sp.level
                            WHEN 'CHIEF' THEN 4
                            WHEN 'SENIOR' THEN 3
                            WHEN 'INTERMEDIATE' THEN 2
                            WHEN 'JUNIOR' THEN 1
                            ELSE 0
                        END DESC,
                        sp.consultation_fee ASC,
                        sp.id DESC
                </otherwise>
            </choose>
            </script>
            """)
    IPage<SpecialistSummaryVO> searchSpecialists(Page<SpecialistSummaryVO> page,
                                                 @Param("query") SpecialistSearchQueryDTO query,
                                                 @Param("today") LocalDate today,
                                                 @Param("currentTime") LocalTime currentTime);

    @Select("""
            SELECT
                sp.id AS id,
                sp.user_id AS userId,
                COALESCE(NULLIF(u.full_name, ''), u.email) AS name,
                COALESCE(sp.avatar_url, '') AS avatarUrl,
                sp.category_id AS categoryId,
                COALESCE(c.category_name, '') AS categoryName,
                COALESCE(sp.level, '') AS level,
                sp.consultation_fee AS consultationFee,
                COALESCE(sp.bio, '') AS bio,
                COALESCE(sp.status, '') AS status,
                COALESCE(u.email, '') AS email,
                COALESCE(u.phone_number, '') AS phoneNumber
            FROM specialist_profiles sp
            INNER JOIN users u ON u.id = sp.user_id
            LEFT JOIN expertise_categories c ON c.id = sp.category_id
            WHERE sp.id = #{id}
            """)
    SpecialistDetailVO getSpecialistDetail(@Param("id") Long id);

    @Select("""
            SELECT
                ts.id AS id,
                ts.slot_date AS slotDate,
                ts.start_time AS startTime,
                ts.end_time AS endTime,
                ts.status AS status
            FROM time_slots ts
            WHERE ts.specialist_id = #{specialistId}
              AND ts.slot_date = #{date}
              AND ts.status IN ('AVAILABLE', 'BOOKED', 'LOCKED')
              AND (
                  ts.slot_date > #{today}
                  OR (ts.slot_date = #{today} AND ts.start_time > #{currentTime})
              )
            ORDER BY ts.start_time ASC
            """)
    List<SpecialistAvailabilityVO> listAvailabilityByDate(@Param("specialistId") Long specialistId,
                                                          @Param("date") LocalDate date,
                                                          @Param("today") LocalDate today,
                                                          @Param("currentTime") LocalTime currentTime);
}
