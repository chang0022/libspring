package me.jiangcai.lib.sys.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Objects;

/**
 * 用于系统保存信息
 * 最多可以保存的长度为255
 *
 * @author CJ
 * @since 3.0
 */
@Entity
public class SystemString {
    @Id
    @Column(length = 50)
    private String id;
    private String value;
    /**
     * java 全限定名称;如果为null则不支持UI更改
     */
    @Column(length = 100)
    private String javaTypeName;
    /**
     * 是否支持运行时更改还是必须重新启动
     */
    private boolean runtime;
    /**
     * 是否运行定制
     */
    private boolean custom;
    /**
     * 更多备注
     */
    @Column(length = 50)
    private String comment;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getJavaTypeName() {
        return javaTypeName;
    }

    public void setJavaTypeName(String javaTypeName) {
        this.javaTypeName = javaTypeName;
    }

    public boolean isRuntime() {
        return runtime;
    }

    public void setRuntime(boolean runtime) {
        this.runtime = runtime;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SystemString)) return false;
        SystemString that = (SystemString) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SystemString{" +
                "id='" + id + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
