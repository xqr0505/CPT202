package edu.xjtlu.cpt202.backend.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author QiranXiao
 * @date 2026/3/26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    private long total;

    private List<T> records;

    private int pageNum;

    private int pageSize;

}
