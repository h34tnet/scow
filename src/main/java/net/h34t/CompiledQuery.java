package net.h34t;

public class CompiledQuery {

    private final String javaSource;

    private final String classname;

    private final String packageName;

    public CompiledQuery(String javaSource, String classname, String packageName) {
        this.javaSource = javaSource;
        this.classname = classname;
        this.packageName = packageName;
    }

    public String getJavaSource() {
        return javaSource;
    }

    public String getClassname() {
        return classname;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public String toString() {
        return "CompiledQuery{" +
                "javaSource='" + javaSource + '\'' +
                ", classname='" + classname + '\'' +
                ", packageName='" + packageName + '\'' +
                '}';
    }
}
