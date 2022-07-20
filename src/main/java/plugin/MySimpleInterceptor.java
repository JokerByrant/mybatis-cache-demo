package plugin;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

/**
 * 实现自定义的插件拦截器
 * @author sxh
 * @date 2022/7/20
 */
// @Intercepts({@Signature(type = Executor.class, method ="update", args = {MappedStatement.class, Object.class})})
@Intercepts({@Signature(type = Executor.class, method ="query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class MySimpleInterceptor implements Interceptor {
    public Object intercept(Invocation invocation) throws Throwable {
        System.out.println("MySimpleInterceptor 被触发啦！");
        // //通过invocation.proceed()方法完成调用链的推进
        return invocation.proceed();
    }

    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    public void setProperties(Properties properties) {

    }
}
