import lombok.Getter;
import lombok.Setter;

/*
*   这个bean就是解析log参数以后 赋值给的对象
*   定义属性 属性的数量跟业务需要有关
*/
public class HttpLogRecord {
        /*
        * @Getter @Setter自动生成 get set 方法
        * 需要安装 lombok-plugin-0.28-2019.3.zip 插件
        */
    @Getter @Setter private String connectionClientUser = null;
    @Getter @Setter private String connectionClientHost = null;
    @Getter @Setter private String method = null;
    @Getter @Setter private String status = null;

}
