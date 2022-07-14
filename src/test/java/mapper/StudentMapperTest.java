package mapper;

import entity.StudentEntity;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

public class StudentMapperTest {

    private SqlSessionFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader("mybatis-config.xml"));

    }

    @Test
    public void showDefaultCacheConfiguration() {
        System.out.println("本地缓存范围: " + factory.getConfiguration().getLocalCacheScope());
        System.out.println("二级缓存是否被启用: " + factory.getConfiguration().isCacheEnabled());
    }

    @Test
    public void testEnumTypeHandler() {
        SqlSession sqlSession = factory.openSession(true); // 自动提交事务
        StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);
        // 重复执行同一sql，后续查询会命中mybatis缓存
        System.out.println(studentMapper.selectAll());
        sqlSession.close();
    }

    /**
     *   一级缓存测试
     *   <setting name="localCacheScope" value="SESSION"/>
     *   <setting name="cacheEnabled" value="true"/>
     * @throws Exception
     */
    @Test
    public void testLocalCache() throws Exception {
        SqlSession sqlSession = factory.openSession(true); // 自动提交事务
        StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);
        // 重复执行同一sql，后续查询会命中mybatis缓存
        System.out.println(studentMapper.getStudentById(1));
        System.out.println(studentMapper.getStudentById(1));
        System.out.println(studentMapper.getStudentById(1));

        sqlSession.close();
    }

    /**
     *  一级缓存测试：insert/update 会清除一级缓存
     *  <setting name="localCacheScope" value="SESSION"/>
     *  <setting name="cacheEnabled" value="true"/>
     * @throws Exception
     */
    @Test
    public void testLocalCacheClear() throws Exception {
        SqlSession sqlSession = factory.openSession(true); // 自动提交事务
        StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);

        System.out.println(studentMapper.getStudentById(1));
        System.out.println("增加了" + studentMapper.addStudent(buildStudent()) + "个学生");
        System.out.println(studentMapper.getStudentById(1));

        sqlSession.close();
    }

    /**
     *   一级缓存测试，别的sqlSession中修改数据，当前sqlSession中的一级缓存不会更新，会读到脏数据
     *   <setting name="localCacheScope" value="SESSION"/>
     *   <setting name="cacheEnabled" value="true"/>
     * @throws Exception
     */
    @Test
    public void testLocalCacheScope() throws Exception {
        SqlSession sqlSession1 = factory.openSession(true); // 自动提交事务
        SqlSession sqlSession2 = factory.openSession(true); // 自动提交事务

       StudentMapper studentMapper = sqlSession1.getMapper(StudentMapper.class);
       StudentMapper studentMapper2 = sqlSession2.getMapper(StudentMapper.class);

        System.out.println("studentMapper读取数据: " + studentMapper.getStudentById(1));
        System.out.println("studentMapper读取数据: " + studentMapper.getStudentById(1));
        System.out.println("studentMapper2更新了" + studentMapper2.updateStudentName("小岑",1) + "个学生的数据");
        System.out.println("studentMapper读取数据: " + studentMapper.getStudentById(1));
        System.out.println("studentMapper2读取数据: " + studentMapper2.getStudentById(1));

    }


    private StudentEntity buildStudent(){
        StudentEntity studentEntity = new StudentEntity();
        studentEntity.setName("明明");
        studentEntity.setAge(20);
        return studentEntity;
    }

    /**
     *  二级缓存测试，查询完没有commit()，二级缓存不会生效
     *  <setting name="localCacheScope" value="SESSION"/>
     *  <setting name="cacheEnabled" value="true"/>
     * @throws Exception
     */
    @Test
    public void testCacheWithoutCommitOrClose() throws Exception {
        SqlSession sqlSession1 = factory.openSession(true); // 自动提交事务
        SqlSession sqlSession2 = factory.openSession(true); // 自动提交事务

        StudentMapper studentMapper = sqlSession1.getMapper(StudentMapper.class);
        StudentMapper studentMapper2 = sqlSession2.getMapper(StudentMapper.class);

        System.out.println("studentMapper读取数据: " + studentMapper.getStudentById(1));
        System.out.println("studentMapper2读取数据: " + studentMapper2.getStudentById(1));

    }

    /**
     *  二级缓存测试，查询之后关闭sqlSession，二级缓存生效
     *  <setting name="localCacheScope" value="SESSION"/>
     *  <setting name="cacheEnabled" value="true"/>
     * @throws Exception
     */
    @Test
    public void testCacheWithCommitOrClose() throws Exception {
        SqlSession sqlSession1 = factory.openSession(true); // 自动提交事务
        SqlSession sqlSession2 = factory.openSession(true); // 自动提交事务

        StudentMapper studentMapper = sqlSession1.getMapper(StudentMapper.class);
        StudentMapper studentMapper2 = sqlSession2.getMapper(StudentMapper.class);

        System.out.println("studentMapper读取数据: " + studentMapper.getStudentById(1));
        sqlSession1.close();
        System.out.println("studentMapper2读取数据: " + studentMapper2.getStudentById(1));

    }

    /**
     *  二级缓存测试，测试 insert/update 方法会清除二级缓存
     *  <setting name="localCacheScope" value="SESSION"/>
     *  <setting name="cacheEnabled" value="true"/>
     * @throws Exception
     */
    @Test
    public void testCacheWithUpdate() throws Exception {
        SqlSession sqlSession1 = factory.openSession(true); // 自动提交事务
        SqlSession sqlSession2 = factory.openSession(true); // 自动提交事务
        SqlSession sqlSession3 = factory.openSession(true); // 自动提交事务


        StudentMapper studentMapper = sqlSession1.getMapper(StudentMapper.class);
        StudentMapper studentMapper2 = sqlSession2.getMapper(StudentMapper.class);
        StudentMapper studentMapper3 = sqlSession3.getMapper(StudentMapper.class);


        System.out.println("studentMapper读取数据: " + studentMapper.getStudentById(1));
        sqlSession1.close();
        System.out.println("studentMapper2读取数据: " + studentMapper2.getStudentById(1));

        studentMapper3.updateStudentName("方方",1);
        sqlSession3.close();
        System.out.println("studentMapper2读取数据: " + studentMapper2.getStudentById(1));
    }

    /**
     *  二级缓存测试：MyBatis的二级缓存不适应用于映射文件中存在多表查询的情况，多表查询会查询到脏数据
     *  MyBatis的二级缓存是基于namespace的，多表查询语句所在的namspace，无法感应到其他namespace中的语句对多表查询中涉及的表进行的修改，引发脏数据问题
     *  <setting name="localCacheScope" value="SESSION"/>
     *  <setting name="cacheEnabled" value="true"/>
     * @throws Exception
     */
    @Test
    public void testCacheWithDiffererntNamespace() throws Exception {
        SqlSession sqlSession1 = factory.openSession(true); // 自动提交事务
        SqlSession sqlSession2 = factory.openSession(true); // 自动提交事务
        SqlSession sqlSession3 = factory.openSession(true); // 自动提交事务


        StudentMapper studentMapper = sqlSession1.getMapper(StudentMapper.class);
        StudentMapper studentMapper2 = sqlSession2.getMapper(StudentMapper.class);
        ClassMapper classMapper = sqlSession3.getMapper(ClassMapper.class);


        System.out.println("studentMapper读取数据: " + studentMapper.getStudentByIdWithClassInfo(1));
        sqlSession1.close();

        System.out.println("studentMapper2读取数据: " + studentMapper2.getStudentByIdWithClassInfo(1));

        classMapper.updateClassName("特色一班",1);
        sqlSession3.commit();

        // 这里会查询到脏数据
        System.out.println("studentMapper2读取数据: " + studentMapper2.getStudentByIdWithClassInfo(1));
    }

    /**
     *  配置 ClassMapper.xml 中 cache-ref 指定 StudentMapper.xml 的缓存，可以发现脏数据的问题没有出现
     *  <setting name="localCacheScope" value="SESSION"/>
     *  <setting name="cacheEnabled" value="true"/>
     * @throws Exception
     */
    @Test
    public void testCacheWithDiffererntNamespaceWithCacheRef() throws Exception {
        SqlSession sqlSession1 = factory.openSession(true); // 自动提交事务
        SqlSession sqlSession2 = factory.openSession(true); // 自动提交事务
        SqlSession sqlSession3 = factory.openSession(true); // 自动提交事务


        StudentMapper studentMapper = sqlSession1.getMapper(StudentMapper.class);
        StudentMapper studentMapper2 = sqlSession2.getMapper(StudentMapper.class);
        ClassMapper classMapper = sqlSession3.getMapper(ClassMapper.class);


        System.out.println("studentMapper读取数据: " + studentMapper.getStudentByIdWithClassInfo(1));
        sqlSession1.close();

        System.out.println("studentMapper2读取数据: " + studentMapper2.getStudentByIdWithClassInfo(1));

        classMapper.updateClassName("特色一班",1);
        sqlSession3.commit();

        System.out.println("studentMapper2读取数据: " + studentMapper2.getStudentByIdWithClassInfo(1));
    }


}
