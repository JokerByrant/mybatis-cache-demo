package typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 自定义TypeHandler
 * @author sxh
 * @date 2022/7/14
 */
// 此处如果不用注解指定jdbcType, 那么，就可以在配置文件中通过"jdbcType"属性指定， 同理， javaType 也可通过 @MappedTypes指定
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(String.class)
public class MySimpleTypeHandler extends BaseTypeHandler<String> {
    /*
        这个方法是在往数据库插入时，将java类型数据转换为jdbc数据的过程
     */
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter + "@自定义Handler存储");
    }

    /*
        下面三个方法是将从数据库查询出的数据，转换为指定数据的处理过程
     */
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName).split("@")[0] + "@自定义Handler返回";
    }

    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex).split("@")[0] + "@自定义Handler返回";
    }

    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex).split("@")[0] + "@自定义Handler返回";
    }
}
