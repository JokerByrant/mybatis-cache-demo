package typehandler.enumTypeHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 自定义EnumTypeHandler，将查询出的值转换为Enum类型
 * @author sxh
 * @date 2022/7/14
 */
@MappedTypes(MySimpleEnum.class)
public class MyEnumTypeHandler extends BaseTypeHandler<MySimpleEnum> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, MySimpleEnum parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public MySimpleEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : codeOf(code);
    }

    @Override
    public MySimpleEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : codeOf(code);
    }

    @Override
    public MySimpleEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : codeOf(code);
    }

    /**
     * code转Enum
     * @param code
     * @return
     */
    private MySimpleEnum codeOf(int code){
        try {
            for (MySimpleEnum e : MySimpleEnum.values()) {
                if (e.getCode() == code)
                    return e;
            }
            return MySimpleEnum.UNKNOWN;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot convert " + code + " to by code value.", ex);
        }
    }
}
