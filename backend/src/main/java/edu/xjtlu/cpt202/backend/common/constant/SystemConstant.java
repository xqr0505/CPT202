package edu.xjtlu.cpt202.backend.common.constant;

/**
 * General System-wide Constants.
 * @author QiranXiao
 * @date 2026/3/31
 */
public class SystemConstant {
    /**
     * The default number of items to display per page in paginated lists.
     */
    public static final Integer DEFAULT_PAGE_SIZE = 10;

    /**
     * A safety upper limit for pagination page size, preventing excessive data retrieval.
     */
    public static final Integer MAX_PAGE_SIZE = 100;

    /**
     * Generic representation for a "true" or "enabled" boolean-like status in database fields.
     */
    public static final Integer TRUE_FLAG = 1;

    /**
     * Generic representation for a "false" or "disabled" boolean-like status in database fields.
     */
    public static final Integer FALSE_FLAG = 0;

    /**
     * Default field for sorting query results if not specified.
     */
    public static final String DEFAULT_ORDER_BY_FIELD = "id";

    /**
     * Default sorting direction for query results (e.g., for created_at or id).
     */
    public static final String DEFAULT_ORDER_BY_DIRECTION = "DESC";
}

