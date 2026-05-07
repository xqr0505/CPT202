package edu.xjtlu.cpt202.backend.modules.ai.mapper;

import edu.xjtlu.cpt202.backend.modules.ai.model.vo.AiSpecialistSearchIndexDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Read model for syncing specialist search documents into Redis.
 *
 * @author Codex
 * @since 2026/5/7
 */
@Mapper
public interface AiSpecialistSearchSyncMapper {

    @Select("""
            SELECT
                sp.id AS specialistId,
                COALESCE(NULLIF(u.full_name, ''), u.email) AS specialistName,
                COALESCE(c.category_name, '') AS categoryName,
                COALESCE(sp.level, '') AS level,
                COALESCE(sp.bio, '') AS bio,
                COALESCE(sp.status, '') AS status
            FROM specialist_profiles sp
            INNER JOIN users u ON u.id = sp.user_id
            LEFT JOIN expertise_categories c ON c.id = sp.category_id
            WHERE sp.id = #{specialistId}
            """)
    AiSpecialistSearchIndexDocument selectBySpecialistId(@Param("specialistId") Long specialistId);

    @Select("""
            SELECT
                sp.id AS specialistId,
                COALESCE(NULLIF(u.full_name, ''), u.email) AS specialistName,
                COALESCE(c.category_name, '') AS categoryName,
                COALESCE(sp.level, '') AS level,
                COALESCE(sp.bio, '') AS bio,
                COALESCE(sp.status, '') AS status
            FROM specialist_profiles sp
            INNER JOIN users u ON u.id = sp.user_id
            LEFT JOIN expertise_categories c ON c.id = sp.category_id
            ORDER BY sp.id ASC
            """)
    List<AiSpecialistSearchIndexDocument> selectAll();
}
