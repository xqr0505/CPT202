package edu.xjtlu.cpt202.backend.modules.specialist.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminSpecialistMapper {

    @Select("""
            <script>
            SELECT
                sp.id AS id,
                COALESCE(NULLIF(u.full_name, ''), u.email) AS name,
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
}
