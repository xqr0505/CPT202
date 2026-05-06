package edu.xjtlu.cpt202.backend.modules.specialist.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminSpecialistMapper {

    @Select("""
            <script>
            SELECT
                sp.id AS id,
                COALESCE(NULLIF(u.full_name, ''), u.email) AS name,
                u.email AS email,
                COALESCE(sp.avatar_url, '') AS avatarUrl,
                sp.category_id AS categoryId,
                COALESCE(c.category_name, '') AS categoryName,
                COALESCE(sp.level, '') AS level,
                sp.consultation_fee AS consultationFee,
                CASE
                    WHEN UPPER(COALESCE(sp.status, '')) = 'ACTIVE' THEN 'Active'
                    WHEN UPPER(COALESCE(sp.status, '')) = 'INACTIVE' THEN 'Inactive'
                    WHEN COALESCE(sp.status, '') = 'Active' THEN 'Active'
                    WHEN COALESCE(sp.status, '') = 'Inactive' THEN 'Inactive'
                    ELSE 'Inactive'
                END AS status,
                EXISTS(
                    SELECT 1
                    FROM bookings b
                    WHERE b.specialist_id = sp.id
                      AND b.status IN ('PENDING', 'CONFIRMED')
                ) AS hasActiveBookings,
                (
                    SELECT COUNT(1)
                    FROM bookings b
                    WHERE b.specialist_id = sp.id
                      AND b.status IN ('PENDING', 'CONFIRMED')
                ) AS activeBookingCount,
                sp.created_at AS createTime
            FROM specialist_profiles sp
            INNER JOIN users u ON u.id = sp.user_id
            LEFT JOIN expertise_categories c ON c.id = sp.category_id
            <where>
                <if test="query.keyword != null and query.keyword != ''">
                    AND (
                        u.full_name LIKE CONCAT('%', #{query.keyword}, '%')
                        OR u.email LIKE CONCAT('%', #{query.keyword}, '%')
                    )
                </if>
                <if test="query.categoryId != null">
                    AND sp.category_id = #{query.categoryId}
                </if>
                <if test="query.status != null and query.status != ''">
                    AND (
                        (#{query.status} = 'Active' AND UPPER(COALESCE(sp.status, '')) = 'ACTIVE')
                        OR
                        (#{query.status} = 'Inactive' AND UPPER(COALESCE(sp.status, '')) = 'INACTIVE')
                    )
                </if>
            </where>
            ORDER BY sp.created_at DESC, sp.id DESC
            </script>
            """)
    IPage<AdminSpecialistListVO> pageSpecialists(
            Page<AdminSpecialistListVO> page,
            @Param("query") AdminSpecialistListQueryDTO query
    );

    @Select("""
            SELECT
                sp.id AS id,
                COALESCE(NULLIF(u.full_name, ''), u.email) AS name,
                u.email AS email,
                COALESCE(sp.avatar_url, '') AS avatarUrl,
                COALESCE(sp.bio, '') AS bio,
                sp.category_id AS categoryId,
                COALESCE(c.category_name, '') AS categoryName,
                COALESCE(sp.level, '') AS level,
                sp.consultation_fee AS consultationFee,
                CASE
                    WHEN UPPER(COALESCE(sp.status, '')) = 'ACTIVE' THEN 'Active'
                    WHEN UPPER(COALESCE(sp.status, '')) = 'INACTIVE' THEN 'Inactive'
                    WHEN COALESCE(sp.status, '') = 'Active' THEN 'Active'
                    WHEN COALESCE(sp.status, '') = 'Inactive' THEN 'Inactive'
                    ELSE 'Inactive'
                END AS status
            FROM specialist_profiles sp
            INNER JOIN users u ON u.id = sp.user_id
            LEFT JOIN expertise_categories c ON c.id = sp.category_id
            WHERE sp.id = #{id}
            """)
    AdminSpecialistDetailVO selectSpecialistDetailById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM expertise_categories
            WHERE id = #{categoryId}
            """)
    Long selectCategoryCountById(@Param("categoryId") Long categoryId);

    @Select("""
            SELECT user_id
            FROM specialist_profiles
            WHERE id = #{specialistId}
            """)
    Long selectUserIdBySpecialistId(@Param("specialistId") Long specialistId);

    @Select("""
            SELECT
                u.id,
                u.email,
                u.password_hash,
                u.role,
                u.status,
                u.full_name,
                u.phone_number,
                u.login_fail_count,
                u.lock_time,
                u.first_fail_time,
                u.created_at,
                u.updated_at,
                u.deleted_at
            FROM users u
            INNER JOIN specialist_profiles sp ON sp.user_id = u.id
            WHERE sp.id = #{specialistId}
            """)
    User selectUserBySpecialistId(@Param("specialistId") Long specialistId);

    @Update("""
            UPDATE specialist_profiles
            SET
                category_id = #{categoryId},
                level = #{level},
                consultation_fee = #{consultationFee},
                avatar_url = #{avatarUrl},
                bio = #{bio},
                status = #{status},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int updateSpecialistProfileById(
            @Param("id") Long id,
            @Param("categoryId") Long categoryId,
            @Param("level") String level,
            @Param("consultationFee") java.math.BigDecimal consultationFee,
            @Param("avatarUrl") String avatarUrl,
            @Param("bio") String bio,
            @Param("status") String status
    );

    @Update("""
            <script>
            UPDATE users
            <set>
                full_name = #{fullName},
                email = #{email},
                avatar_url = #{avatarUrl},
                <if test="passwordHash != null and passwordHash != ''">
                    password_hash = #{passwordHash},
                    password_changed_at = NOW(),
                </if>
                updated_at = NOW()
            </set>
            WHERE id = #{userId}
            </script>
            """)
    int updateUserAccountById(
            @Param("userId") Long userId,
            @Param("fullName") String fullName,
            @Param("email") String email,
            @Param("avatarUrl") String avatarUrl,
            @Param("passwordHash") String passwordHash
    );

    @Update("""
            UPDATE specialist_profiles
            SET
                status = #{status},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int updateSpecialistStatusById(
            @Param("id") Long id,
            @Param("status") String status
    );
}
