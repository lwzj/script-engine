package com.lw.commons.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl;

import java.io.File;
import java.io.FileFilter;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/***
 *                    .::::.
 *                  .::::::::.
 *                 :::::::::::  
 *             ..:::::::::::'
 *           '::::::::::::'
 *             .::::::::::
 *        '::::::::::::::..
 *             ..::::::::::::.
 *           ``::::::::::::::::
 *            ::::``:::::::::'        .:::.
 *           ::::'   ':::::'       .::::::::.
 *         .::::'      ::::     .:::::::'::::.
 *        .:::'       :::::  .:::::::::' ':::::.
 *       .::'        :::::.:::::::::'      ':::::.
 *      .::'         ::::::::::::::'         ``::::.
 *  ...:::           ::::::::::::'              ``::.
 * ```` ':.          ':::::::::'                  ::::..
 *                    '.:::::'                    ':'````..
 *  @data 2020/10/16 10:26
 *  @author lw
 */
public class ClassUtil {
    private static final String PROTOCOL_FILE = "file";
    private static final String PROTOCOL_JAR = "jar";
    private static Logger LOGGER = LoggerFactory.getLogger(ClassUtil.class);

    public ClassUtil() {
    }

    public static Set<Class<?>> findClassWithAnnotation(ClassLoader classLoader, String packageName, Class<? extends Annotation> annotationClass) {
        Set<Class<?>> ret = getFromClassPath(classLoader, packageName, (clazzx) -> {
            return clazzx.getAnnotation(annotationClass) != null;
        });
        Iterator var4 = ret.iterator();

        while(var4.hasNext()) {
            Class<?> clazz = (Class)var4.next();
            LOGGER.info("找到持有[{}]注解的类：{}", annotationClass.getName(), clazz.getName());
        }

        return ret;
    }

    public static Set<Class<?>> findClassWithAnnotation(ClassLoader classLoader, String packageName, JarFile file, Class<? extends Annotation> annotationClass) {
        Set<Class<?>> ret = getClassFromJarFile(classLoader, packageName, file, (clazzx) -> {
            return clazzx.getAnnotation(annotationClass) != null;
        });
        Iterator var5 = ret.iterator();

        while(var5.hasNext()) {
            Class<?> clazz = (Class)var5.next();
            LOGGER.info("找到持有[{}]注解的类：{}", annotationClass.getName(), clazz.getName());
        }

        return ret;
    }

    public static Set<Class<?>> findClassWithSuperClass(String packageName, Class<?> parentClass) {
        Set<Class<?>> ret = getFromClassPath((ClassLoader)null, packageName, (clazzx) -> {
            return parentClass.isAssignableFrom(clazzx) && !parentClass.equals(clazzx);
        });
        Iterator var3 = ret.iterator();

        while(var3.hasNext()) {
            Class<?> clazz = (Class)var3.next();
            LOGGER.info("发现[{}]的子类：{}", parentClass.getName(), clazz.getName());
        }

        return ret;
    }

    public static Set<Class<?>> getFromClassPath(ClassLoader classLoader, String packageName, ClassUtil.ClassSelector selector) {
        if (classLoader == null) {
            classLoader = ClassUtil.class.getClassLoader();
        }

        Set<Class<?>> allClazz = new LinkedHashSet();
        String packageDir = packageName.replace('.', File.separatorChar);

        try {
            Enumeration dirs = classLoader.getResources(packageDir);

            while(dirs.hasMoreElements()) {
                URL url = (URL)dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    allClazz.addAll(findClassFromDir(classLoader, packageName, filePath));
                } else if ("jar".equals(protocol)) {
                    JarFile jar = ((JarURLConnection)url.openConnection()).getJarFile();
                    allClazz.addAll(findClassFromJar(classLoader, jar, packageDir));
                }
            }
        } catch (Throwable var9) {
            LOGGER.error("读取日志Class文件出错", var9);
        }

        Set<Class<?>> ret = new LinkedHashSet();
        Iterator var11 = allClazz.iterator();

        while(var11.hasNext()) {
            Class<?> clazz = (Class)var11.next();
            if (selector.select(clazz)) {
                ret.add(clazz);
            }
        }

        return ret;
    }

    private static Set<Class<?>> getClassFromJarFile(ClassLoader classLoader, String packageName, JarFile file, ClassUtil.ClassSelector selector) {
        if (classLoader == null) {
            classLoader = ClassUtil.class.getClassLoader();
        }

        String packageDir = packageName.replace('.', '/');
        Set<Class<?>> allClazz = findClassFromJar(classLoader, file, packageDir);
        Set<Class<?>> ret = new LinkedHashSet();
        Iterator var7 = allClazz.iterator();

        while(var7.hasNext()) {
            Class<?> clazz = (Class)var7.next();
            if (selector.select(clazz)) {
                ret.add(clazz);
            }
        }

        return ret;
    }

    private static Set<Class<?>> findClassFromJar(ClassLoader classLoader, JarFile jar, String packageDir) {
        Set<Class<?>> ret = new LinkedHashSet();
        Enumeration entries = jar.entries();

        while(entries.hasMoreElements()) {
            JarEntry entry = (JarEntry)entries.nextElement();
            if (!entry.isDirectory()) {
                String name = entry.getName();
                if (name.startsWith(packageDir) && name.endsWith(".class")) {
                    name = name.replaceAll("/", ".");
                    name = name.substring(0, name.length() - 6);

                    try {
                        Class<?> clazz = Class.forName(name, false, classLoader);
                        ret.add(clazz);
                    } catch (Throwable var8) {
                        LOGGER.error("读取Jar中的Class文件出错:" + name, var8);
                    }
                }
            }
        }

        return ret;
    }

    private static Set<Class<?>> findClassFromDir(ClassLoader classLoader, String packageName, String filePath) {
        File dir = new File(filePath);
        if (dir.exists() && dir.isDirectory()) {
            Set<Class<?>> ret = new LinkedHashSet();
            File[] files = dir.listFiles((filex) -> {
                return filex.isDirectory() || filex.getName().endsWith(".class");
            });
            File[] var6 = files;
            int var7 = files.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                File file = var6[var8];
                if (file.isDirectory()) {
                    ret.addAll(findClassFromDir(classLoader, packageName + "." + file.getName(), file.getAbsolutePath()));
                } else {
                    String className = file.getName().substring(0, file.getName().length() - 6);

                    try {
                        Class<?> clazz = Class.forName(packageName + '.' + className, false, classLoader);
                        ret.add(clazz);
                    } catch (Throwable var12) {
                        LOGGER.error("读取文件夹中的Class文件出错", var12);
                    }
                }
            }

            return ret;
        } else {
            return Collections.emptySet();
        }
    }

    public static List<Class<?>> getFieldParameterizedTypeClass(Field field) {
        Type genericType = field.getGenericType();
        List<Class<?>> ret = new ArrayList(0);
        if (genericType != null) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)genericType;
                ret = getActualTypeArgumentClass(parameterizedType);
            } else if (genericType instanceof GenericArrayType) {
                GenericArrayType genericArrayType = (GenericArrayTypeImpl)genericType;
                ParameterizedType parameterizedType = (ParameterizedType)genericArrayType.getGenericComponentType();
                ret = getActualTypeArgumentClass(parameterizedType);
            }
        }

        return (List)ret;
    }

    private static List<Class<?>> getActualTypeArgumentClass(ParameterizedType parameterizedType) {
        List<Class<?>> ret = new ArrayList();
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

        for(int i = 0; i < actualTypeArguments.length; ++i) {
            Type actualTypeArgument = actualTypeArguments[i];
            if (actualTypeArgument instanceof ParameterizedType) {
                ParameterizedType childParameterizedType = (ParameterizedType)actualTypeArgument;
                ret.add((Class)childParameterizedType.getRawType());
                ret.addAll(getActualTypeArgumentClass(childParameterizedType));
            } else {
                if (actualTypeArgument instanceof WildcardType) {
                    throw new RuntimeException("存储类中不允许使用通配符->field:" + parameterizedType.getTypeName());
                }

                Class<?> clazz = (Class)actualTypeArgument;
                ret.add(clazz);
            }
        }

        return ret;
    }

    public interface ClassSelector {
        boolean select(Class<?> var1);
    }
}
