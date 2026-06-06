package com.blog.util;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 分页工具类
 * 
 * 【面试考点】
 * 1. 分页结果封装
 * 2. 泛型设计
 */
public class PageUtil {

    /**
     * 将 IPage 转换为普通 Page 对象
     * 
     * @param source 源分页对象
     * @param <T> 数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> toPageResult(IPage<T> source) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setCurrent(source.getCurrent());
        result.setSize(source.getSize());
        result.setRecords(source.getRecords());
        return result;
    }

    /**
     * 分页结果类
     */
    public static class PageResult<T> {
        /**
         * 总记录数
         */
        private Long total;

        /**
         * 总页数
         */
        private Long pages;

        /**
         * 当前页码
         */
        private Long current;

        /**
         * 每页大小
         */
        private Long size;

        /**
         * 数据列表
         */
        private List<T> records;

        // Getters and Setters
        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public Long getPages() {
            return pages;
        }

        public void setPages(Long pages) {
            this.pages = pages;
        }

        public Long getCurrent() {
            return current;
        }

        public void setCurrent(Long current) {
            this.current = current;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public List<T> getRecords() {
            return records;
        }

        public void setRecords(List<T> records) {
            this.records = records;
        }
    }
}
