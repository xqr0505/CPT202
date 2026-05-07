package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiSpecialistSearchProperties;
import edu.xjtlu.cpt202.backend.modules.ai.model.vo.AiSpecialistSearchItemVO;
import edu.xjtlu.cpt202.backend.modules.ai.model.vo.AiSpecialistSearchResultVO;
import edu.xjtlu.cpt202.backend.modules.ai.rag.RagProperties;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSpecialistSearchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis-backed hybrid specialist retrieval service.
 *
 * @author Codex
 * @since 2026/5/7
 */
@Service
@ConditionalOnProperty(prefix = "ai.search.specialist", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiSpecialistSearchServiceImpl implements AiSpecialistSearchService {

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_LEVEL = "level";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_VECTOR = "vector";
    private static final String FIELD_VECTOR_SCORE_ALIAS = "vector_score";

    private final EmbeddingModel embeddingModel;
    private final AiSpecialistSearchProperties properties;
    private final JedisPooled specialistSearchJedis;

    public AiSpecialistSearchServiceImpl(
            EmbeddingModel embeddingModel,
            AiSpecialistSearchProperties properties,
            RagProperties ragProperties
    ) {
        this(embeddingModel, properties, createJedisClientStatic(ragProperties == null ? null : ragProperties.getRedis()));
    }

    AiSpecialistSearchServiceImpl(
            EmbeddingModel embeddingModel,
            AiSpecialistSearchProperties properties,
            JedisPooled specialistSearchJedis
    ) {
        this.embeddingModel = embeddingModel;
        this.properties = properties;
        this.specialistSearchJedis = specialistSearchJedis;
    }

    @Override
    public AiSpecialistSearchResultVO search(String searchQuery, String categoryFilter, String levelFilter, String nameFilter) {
        String normalizedQuery = trimToNull(searchQuery);
        String normalizedCategory = trimToNull(categoryFilter);
        String normalizedLevel = normalizeUpperCase(levelFilter);
        String normalizedName = trimToNull(nameFilter);

        if (normalizedQuery == null && normalizedName == null) {
            return emptyResult(normalizedQuery, normalizedCategory, normalizedLevel, normalizedName);
        }

        try {
            Map<String, Candidate> candidates = new LinkedHashMap<>();
            mergeKeywordCandidates(candidates, normalizedQuery, normalizedCategory, normalizedLevel, normalizedName);
            mergeVectorCandidates(candidates, normalizedQuery, normalizedCategory, normalizedLevel, normalizedName);

            List<AiSpecialistSearchItemVO> items = candidates.values().stream()
                    .filter(this::passesThreshold)
                    .sorted(candidateComparator())
                    .limit(properties.getMaxResults())
                    .map(this::toItem)
                    .toList();

            return AiSpecialistSearchResultVO.builder()
                    .query(normalizedQuery)
                    .appliedCategoryFilter(normalizedCategory)
                    .appliedLevelFilter(normalizedLevel)
                    .appliedNameFilter(normalizedName)
                    .returnedCount(items.size())
                    .items(items)
                    .build();
        } catch (Exception exception) {
            return emptyResult(normalizedQuery, normalizedCategory, normalizedLevel, normalizedName);
        }
    }

    private void mergeKeywordCandidates(
            Map<String, Candidate> candidates,
            String searchQuery,
            String categoryFilter,
            String levelFilter,
            String nameFilter
    ) {
        String textQuery = buildKeywordQuery(searchQuery, categoryFilter, levelFilter, nameFilter);
        if (textQuery == null) {
            return;
        }

        Query query = new Query(textQuery)
                .setWithScores()
                .limit(0, properties.getTopK())
                .returnFields(FIELD_ID, FIELD_NAME, FIELD_CATEGORY, FIELD_LEVEL, FIELD_CONTENT)
                .dialect(2);
        SearchResult result = specialistSearchJedis.ftSearch(properties.getIndexName(), query);
        if (result == null || result.getDocuments() == null) {
            return;
        }

        double maxScore = result.getDocuments().stream()
                .map(Document::getScore)
                .filter(score -> score != null && score > 0)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0D);

        for (Document document : result.getDocuments()) {
            Candidate candidate = candidates.computeIfAbsent(document.getId(), ignored -> Candidate.fromDocument(document));
            candidate.keywordScore = normalizeKeywordScore(document.getScore(), maxScore);
            candidate.nameMatched = nameFilter != null && candidate.specialistName != null
                    && candidate.specialistName.toLowerCase(Locale.ROOT).contains(nameFilter.toLowerCase(Locale.ROOT));
        }
    }

    private void mergeVectorCandidates(
            Map<String, Candidate> candidates,
            String searchQuery,
            String categoryFilter,
            String levelFilter,
            String nameFilter
    ) {
        if (searchQuery == null) {
            return;
        }
        byte[] vectorBytes = toLittleEndianBytes(embed(searchQuery).vector());
        String redisQuery = buildVectorQuery(categoryFilter, levelFilter);
        Query query = new Query(redisQuery)
                .addParam("K", properties.getTopK())
                .addParam("BLOB", vectorBytes)
                .setSortBy(FIELD_VECTOR_SCORE_ALIAS, true)
                .limit(0, properties.getTopK())
                .returnFields(FIELD_ID, FIELD_NAME, FIELD_CATEGORY, FIELD_LEVEL, FIELD_CONTENT, FIELD_VECTOR_SCORE_ALIAS)
                .dialect(2);
        SearchResult result = specialistSearchJedis.ftSearch(properties.getIndexName(), query);
        if (result == null || result.getDocuments() == null) {
            return;
        }
        for (Document document : result.getDocuments()) {
            Candidate candidate = candidates.computeIfAbsent(document.getId(), ignored -> Candidate.fromDocument(document));
            candidate.semanticScore = similarityFromDistance(valueAsDouble(document.get(FIELD_VECTOR_SCORE_ALIAS)));
            candidate.nameMatched = candidate.nameMatched || (nameFilter != null && candidate.specialistName != null
                    && candidate.specialistName.toLowerCase(Locale.ROOT).contains(nameFilter.toLowerCase(Locale.ROOT)));
        }
    }

    private boolean passesThreshold(Candidate candidate) {
        double threshold = candidate.keywordScore >= 0.75D
                ? properties.getStrongKeywordSimilarityThreshold()
                : properties.getSimilarityThreshold();
        return candidate.semanticScore >= threshold || candidate.keywordScore >= 0.60D;
    }

    private Comparator<Candidate> candidateComparator() {
        return Comparator
                .comparingDouble(this::finalScore)
                .reversed()
                .thenComparingInt(candidate -> levelRank(candidate.level))
                .reversed()
                .thenComparing(candidate -> candidate.specialistId == null ? Long.MAX_VALUE : candidate.specialistId);
    }

    private double finalScore(Candidate candidate) {
        double score = properties.getSemanticWeight() * candidate.semanticScore
                + properties.getKeywordWeight() * candidate.keywordScore;
        if (candidate.nameMatched) {
            score += 0.10D;
        }
        return score;
    }

    private AiSpecialistSearchItemVO toItem(Candidate candidate) {
        return AiSpecialistSearchItemVO.builder()
                .specialistId(candidate.specialistId)
                .specialistName(candidate.specialistName)
                .categoryName(candidate.categoryName)
                .level(candidate.level)
                .matchSummary(summarize(candidate.content))
                .build();
    }

    private AiSpecialistSearchResultVO emptyResult(String query, String category, String level, String name) {
        return AiSpecialistSearchResultVO.builder()
                .query(query)
                .appliedCategoryFilter(category)
                .appliedLevelFilter(level)
                .appliedNameFilter(name)
                .returnedCount(0)
                .items(List.of())
                .build();
    }

    private Embedding embed(String query) {
        Response<Embedding> response = embeddingModel.embed(query);
        if (response == null || response.content() == null || response.content().vector() == null) {
            throw new IllegalStateException("Specialist search embedding result is empty.");
        }
        return response.content();
    }

    private String buildKeywordQuery(String searchQuery, String categoryFilter, String levelFilter, String nameFilter) {
        List<String> clauses = new ArrayList<>();
        String filterClause = buildTagFilterClause(categoryFilter, levelFilter);
        if (filterClause != null) {
            clauses.add(filterClause);
        }
        String textClause = buildTextClause(searchQuery, nameFilter);
        if (textClause != null) {
            clauses.add(textClause);
        }
        if (clauses.isEmpty()) {
            return null;
        }
        return clauses.stream().collect(Collectors.joining(" "));
    }

    private String buildVectorQuery(String categoryFilter, String levelFilter) {
        String filterClause = buildTagFilterClause(categoryFilter, levelFilter);
        String base = filterClause == null ? "*" : filterClause;
        return base + "=>[KNN $K @" + FIELD_VECTOR + " $BLOB AS " + FIELD_VECTOR_SCORE_ALIAS + "]";
    }

    private String buildTagFilterClause(String categoryFilter, String levelFilter) {
        List<String> clauses = new ArrayList<>();
        if (categoryFilter != null) {
            clauses.add("@" + FIELD_CATEGORY + ":{" + escapeTagValue(categoryFilter) + "}");
        }
        if (levelFilter != null) {
            clauses.add("@" + FIELD_LEVEL + ":{" + escapeTagValue(levelFilter) + "}");
        }
        if (clauses.isEmpty()) {
            return null;
        }
        return clauses.stream().collect(Collectors.joining(" "));
    }

    private String buildTextClause(String searchQuery, String nameFilter) {
        List<String> clauses = new ArrayList<>();
        if (searchQuery != null) {
            clauses.add("@" + FIELD_CONTENT + ":(" + escapeText(searchQuery) + ")");
        }
        if (nameFilter != null) {
            clauses.add("@" + FIELD_NAME + ":(" + escapeText(nameFilter) + ")");
        }
        if (clauses.isEmpty()) {
            return null;
        }
        return "(" + String.join(" | ", clauses) + ")";
    }

    private String summarize(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        int bioIndex = content.indexOf("Bio:");
        String bio = bioIndex >= 0 ? content.substring(bioIndex + 4).trim() : content.trim();
        String[] sentences = bio.split("(?<=[.!?])\\s+");
        String summary = sentences.length >= 2
                ? sentences[0] + " " + sentences[1]
                : sentences[0];
        if (summary.length() <= 180) {
            return summary;
        }
        return summary.substring(0, 177).trim() + "...";
    }

    private double normalizeKeywordScore(Double rawScore, double maxScore) {
        if (rawScore == null || rawScore <= 0 || maxScore <= 0) {
            return 0.0D;
        }
        double normalized = rawScore / maxScore;
        return Math.max(0.0D, Math.min(1.0D, normalized));
    }

    private double similarityFromDistance(Double distance) {
        if (distance == null) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, 1.0D - distance));
    }

    private Double valueAsDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private int levelRank(String level) {
        if (level == null) {
            return 0;
        }
        return switch (level.toUpperCase(Locale.ROOT)) {
            case "CHIEF" -> 4;
            case "SENIOR" -> 3;
            case "INTERMEDIATE" -> 2;
            case "JUNIOR" -> 1;
            default -> 0;
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeUpperCase(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String escapeTagValue(String value) {
        return value.replace("\\", "\\\\").replace("-", "\\-").replace(" ", "\\ ");
    }

    private String escapeText(String value) {
        return value.replace("\\", "\\\\")
                .replace("(", " ")
                .replace(")", " ")
                .replace("[", " ")
                .replace("]", " ")
                .trim();
    }

    private byte[] toLittleEndianBytes(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    private static JedisPooled createJedisClientStatic(RagProperties.Redis redis) {
        if (redis == null) {
            return new JedisPooled("127.0.0.1", 6379);
        }
        String username = redis.getUsername();
        String password = redis.getPassword();
        if (username != null && !username.isBlank()) {
            return new JedisPooled(redis.getHost(), redis.getPort(), username, password);
        }
        if (password != null && !password.isBlank()) {
            return new JedisPooled(redis.getHost(), redis.getPort(), null, password);
        }
        return new JedisPooled(redis.getHost(), redis.getPort());
    }

    static class Candidate {
        private final String docId;
        private Long specialistId;
        private String specialistName;
        private String categoryName;
        private String level;
        private String content;
        private double keywordScore;
        private double semanticScore;
        private boolean nameMatched;

        private Candidate(String docId) {
            this.docId = docId;
        }

        static Candidate fromDocument(Document document) {
            Candidate candidate = new Candidate(document.getId());
            candidate.specialistId = parseLong(document.get(FIELD_ID));
            candidate.specialistName = stringValue(document.get(FIELD_NAME));
            candidate.categoryName = stringValue(document.get(FIELD_CATEGORY));
            candidate.level = stringValue(document.get(FIELD_LEVEL));
            candidate.content = stringValue(document.get(FIELD_CONTENT));
            return candidate;
        }

        private static Long parseLong(Object value) {
            if (value == null) {
                return null;
            }
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        private static String stringValue(Object value) {
            return value == null ? null : String.valueOf(value);
        }
    }
}
