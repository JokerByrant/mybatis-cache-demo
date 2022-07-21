package mapper;

import entity.StudentEntity;
import org.apache.ibatis.annotations.Param;
import plugin.pageHelper.PageParameter;

import java.util.List;
import java.util.Map;

// 在xml中定义的配置通过注解也可以配置，如下配置了<cache>和<cache-ref>
// @CacheNamespaceRef(value = ClassMapper.class)
// @CacheNamespace
public interface StudentMapper {

	public StudentEntity getStudentById(int id);

	public int addStudent(StudentEntity student);

	public int updateStudentName(@Param("name") String name, @Param("id") int id);

	public StudentEntity getStudentByIdWithClassInfo(int id);

	List<Map<String, Object>> selectAll();

	/*
		Mybatis在执行mapperElement()解析mapper时，注解和xml都会解析。它不允许sql语句同时出现在注解和xml文件中，在解析阶段会报错。
		原因是解析后的Mapper配置都存放在Configuration中，使用StrictMap存放，这个Map是Configuration的静态内部类，特性是不允许添加重复的key，否则会报错
	 */
	// @Select("select * from student")
	List<StudentEntity> getStudentByAnnotation();

	List<StudentEntity> queryByPage(PageParameter page);
}
