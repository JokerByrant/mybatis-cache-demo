package plugin.pageHelper;

/**
 * @author sxh
 * @date 2022/7/21
 */
public class PageParameter {
    // 要查询的页
    private int currentPage;
    // 每页数量
    private int pageSize;
    // 返回的数据条数
    private int totalCount;
    // 返回的数据页数
    private int totalPage;

    public PageParameter() {
    }

    public PageParameter(int currentPage, int pageSize) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }
}
