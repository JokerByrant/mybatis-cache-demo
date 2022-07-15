package typehandler.enumTypeHandler;

/**
 * @author sxh
 * @date 2022/7/14
 */
public enum MySimpleEnum implements BaseEnum {
    FIRST(1),
    SECOND(2),
    THIRD(3),
    UNKNOWN(-1);

    private int id;

    MySimpleEnum(int id) {
        this.id = id;
    }

    public Integer getCode() {
        return id;
    }
}
