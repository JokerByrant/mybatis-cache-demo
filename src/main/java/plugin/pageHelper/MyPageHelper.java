package plugin.pageHelper;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;


/**
 * 实现自定义的分页组件
 * @author sxh
 * @date 2022/7/21
 */
@Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class}))
public class MyPageHelper implements Interceptor {
    // 数据库类型
    private String dialect;
    // 指定拦截的规则(只拦截与该规则匹配的方法)
    private String pageSqlId;

    private static final ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();
    private static final ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
    private static final String defaultDialect = "mysql";
    private static final String defaultPageSqlId = ".*Page$";

    private static final Logger logger = Logger.getLogger(MyPageHelper.class);

    public Object intercept(Invocation invocation) throws Throwable {
        invokeMyPageHelper(invocation);
        // 将执行权交给下一个拦截器
        return invocation.proceed();
    }

    public Object plugin(Object target) {
        // 当目标类是StatementHandler类型时，才包装目标类，否者直接返回目标本身,减少目标被代理的
        // 次数
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }

    public void setProperties(Properties properties) {

    }

    /**
     * 执行分页
     * @param invocation
     */
    private void invokeMyPageHelper(Invocation invocation) {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaStatementHandler = MetaObject.forObject(statementHandler,
                DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY);
        // 分离代理对象链(由于目标类可能被多个拦截器拦截，从而形成多次代理，通过下面的两次循环
        // 可以分离出最原始的的目标类)
        while (metaStatementHandler.hasGetter("h")) {
            Object object = metaStatementHandler.getValue("h");
            metaStatementHandler = MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY,
                    DEFAULT_OBJECT_WRAPPER_FACTORY);
        }
        // 分离最后一个代理对象的目标类
        while (metaStatementHandler.hasGetter("target")) {
            Object object = metaStatementHandler.getValue("target");
            metaStatementHandler = MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY,
                    DEFAULT_OBJECT_WRAPPER_FACTORY);
        }
        Configuration configuration = (Configuration) metaStatementHandler.
                getValue("delegate.configuration");
        dialect = configuration.getVariables().getProperty("dialect");
        if (null == dialect || "".equals(dialect)) {
            logger.warn("Property dialect is not setted,use default 'mysql' ");
            dialect = defaultDialect;
        }
        pageSqlId = configuration.getVariables().getProperty("pageSqlId");
        if (null == pageSqlId || "".equals(pageSqlId)) {
            logger.warn("Property pageSqlId is not setted,use default '.*Page$' ");
            pageSqlId = defaultPageSqlId;
        }
        MappedStatement mappedStatement = (MappedStatement)
                metaStatementHandler.getValue("delegate.mappedStatement");
        // 只重写需要分页的sql语句。通过MappedStatement的ID匹配，默认重写以Page结尾的
        //  MappedStatement的sql
        if (mappedStatement.getId().matches(pageSqlId)) {
            BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
            Object parameterObject = boundSql.getParameterObject();
            if (parameterObject == null) {
                throw new NullPointerException("parameterObject is null!");
            } else {
                // 分页参数作为参数对象parameterObject的一个属性
                PageParameter page = (PageParameter) parameterObject;
                String sql = boundSql.getSql();
                // 重写sql
                String pageSql = buildPageSql(sql, page);
                metaStatementHandler.setValue("delegate.boundSql.sql", pageSql);
                // 采用物理分页后，就不需要mybatis的内存分页了，所以重置下面的两个参数
                metaStatementHandler.setValue("delegate.rowBounds.offset",
                        RowBounds.NO_ROW_OFFSET);
                metaStatementHandler.setValue("delegate.rowBounds.limit", RowBounds.NO_ROW_LIMIT);
                Connection connection = (Connection) invocation.getArgs()[0];
                // 重设分页参数里的总页数等
                setPageParameter(sql, connection, mappedStatement, boundSql, page);
            }
        }
    }

    /**
     * 重写sql，将分页的参数写进去
     * @param sql
     * @param page
     * @return
     */
    private String buildPageSql(String sql, PageParameter page) {
        if (page != null) {
            StringBuilder pageSql = new StringBuilder();
            if ("mysql".equals(dialect)) {
                pageSql = buildPageSqlForMysql(sql, page);
            } else if ("oracle".equals(dialect)) {
                pageSql = buildPageSqlForOracle(sql, page);
            } else {
                return sql;
            }
            return pageSql.toString();
        } else {
            return sql;
        }
    }

    /**
     * Mysql分页处理
     * @param sql
     * @param page
     * @return
     */
    public StringBuilder buildPageSqlForMysql(String sql, PageParameter page) {
        String beginrow = String.valueOf((page.getCurrentPage() - 1) * page.getPageSize());
        return new StringBuilder(100)
                .append(sql).append(" limit ").append(beginrow).append(",").append(page.getPageSize());
    }


    /**
     * Oracle分页处理
     * @param sql
     * @param page
     * @return
     */
    public StringBuilder buildPageSqlForOracle(String sql, PageParameter page) {
        StringBuilder pageSql = new StringBuilder(100);
        String beginrow = String.valueOf((page.getCurrentPage() - 1) * page.getPageSize());
        String endrow = String.valueOf(page.getCurrentPage() * page.getPageSize());
        pageSql.append("select * from ( select temp.*, rownum row_id from ( ");
        pageSql.append(sql);
        pageSql.append(" ) temp where rownum <= ").append(endrow);
        pageSql.append(") where row_id > ").append(beginrow);
        return pageSql;
    }

    /**
     * 从数据库里查询总的记录数并计算总页数，回写进分页参数<code>PageParameter</code>,这样调用
     * 者就可用通过 分页参数<code>PageParameter</code>获得相关信息。
     *
     * @param sql
     * @param connection
     * @param mappedStatement
     * @param boundSql
     * @param page
     */
    private void setPageParameter(String sql, Connection connection, MappedStatement mappedStatement,
                                  BoundSql boundSql, PageParameter page) {
        // 记录总记录数
        String countSql = "select count(0) from (" + sql + ") as total";
        PreparedStatement countStmt = null;
        ResultSet rs = null;
        try {
            countStmt = connection.prepareStatement(countSql);
            BoundSql countBS = new BoundSql(mappedStatement.getConfiguration(), countSql,
                    boundSql.getParameterMappings(), boundSql.getParameterObject());
            setParameters(countStmt, mappedStatement, countBS, boundSql.getParameterObject());
            // 执行查询，获取返回的记录总数
            rs = countStmt.executeQuery();
            int totalCount = 0;
            if (rs.next()) {
                totalCount = rs.getInt(1);
            }
            page.setTotalCount(totalCount);
            int totalPage = totalCount / page.getPageSize() + ((totalCount % page.getPageSize() == 0) ? 0 : 1);
            page.setTotalPage(totalPage);
        } catch (SQLException e) {
            logger.error("Ignore this exception", e);
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("Ignore this exception", e);
            }
            try {
                countStmt.close();
            } catch (SQLException e) {
                logger.error("Ignore this exception", e);
            }
        }
    }

    /**
     * 对SQL参数(?)设值
     *
     * @param ps
     * @param mappedStatement
     * @param boundSql
     * @param parameterObject
     * @throws SQLException
     */
    private void setParameters(PreparedStatement ps, MappedStatement mappedStatement, BoundSql boundSql,
                               Object parameterObject) throws SQLException {
        ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
        parameterHandler.setParameters(ps);
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public String getPageSqlId() {
        return pageSqlId;
    }

    public void setPageSqlId(String pageSqlId) {
        this.pageSqlId = pageSqlId;
    }
}
