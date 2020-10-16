package com.lw.script;

import com.lw.commons.tuple.TwoTuple;
import com.lw.commons.util.ClassUtil;
import com.lw.script.annotation.Exclude;
import com.lw.script.annotation.Script;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
 *  @data 2020/10/16 10:19
 *  @author lw
 */
public class ScriptEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptEngine.class);
    private static Map<Class<?>, IScript> script_1_to_1 = new HashMap();
    private static Map<Class<?>, List<IScript>> script_1_to_n = new HashMap();
    private static List<Class<?>> fieldCheckIgnoreTypeList = new ArrayList();
    private static boolean dev = false;
    private static String[] scriptJarFile = null;

    public ScriptEngine() {
    }

    public static void setDev(boolean dev) {
        ScriptEngine.dev = dev;
    }

    public static void setScriptJarFile(String[] scriptJarFile) {
        ScriptEngine.scriptJarFile = scriptJarFile;
    }

    public static void addIgnoredStaticFinalFiledType(Class<?> clazz) {
        fieldCheckIgnoreTypeList.add(clazz);
    }

    public static boolean load(String packageName) throws ScriptException {
        TwoTuple<JarFile[], URL[]> tuple = findJarAndURLFromPaths();
        JarFile[] jarFiles = (JarFile[])tuple.first;
        URL[] urls = (URL[])tuple.second;
        if (dev || jarFiles != null && jarFiles.length != 0) {
            ScriptClassLoader classLoader = ScriptClassLoader.newInstance(urls, packageName, dev);
            Object classList;
            if (dev) {
                classList = ClassUtil.findClassWithAnnotation(classLoader, packageName, Script.class);
            } else {
                classList = new HashSet();
                JarFile[] var6 = jarFiles;
                int var7 = jarFiles.length;

                for(int var8 = 0; var8 < var7; ++var8) {
                    JarFile jarFile = var6[var8];
                    Set<Class<?>> classes = ClassUtil.findClassWithAnnotation(classLoader, packageName, jarFile, Script.class);
                    ((Set)classList).addAll(classes);
                }
            }

            return checkAndLoad((Collection)classList, true);
        } else {
            throw new ScriptException("非调试模式下必须通过-Dgame.script.file参数设置脚本jar包所在路径,设置调试模式请使用-Dgame.script.dev=true");
        }
    }

    public static boolean load(String packageName, String[] bootstrapImpls) throws ScriptException {
        TwoTuple<JarFile[], URL[]> tuple = findJarAndURLFromPaths();
        JarFile[] jarFiles = (JarFile[])tuple.first;
        URL[] urls = (URL[])tuple.second;
        if (dev || jarFiles != null && jarFiles.length != 0) {
            ScriptClassLoader classLoader = ScriptClassLoader.newInstance(urls, packageName, dev);
            List<BootstrapScript> bootsScriptList = new ArrayList();
            String[] var7 = bootstrapImpls;
            int var8 = bootstrapImpls.length;

            for(int var9 = 0; var9 < var8; ++var9) {
                String bootstrapImpl = var7[var9];

                try {
                    Class<?> clazz = classLoader.loadClass(bootstrapImpl);
                    BootstrapScript bootsScript = (BootstrapScript)clazz.newInstance();
                    bootsScriptList.add(bootsScript);
                } catch (Exception var13) {
                    throw new RuntimeException("实例化脚本启动实现类发生错误：" + bootstrapImpl, var13);
                }
            }

            List<Class<? extends IScript>> scriptList = new ArrayList();
            Iterator var15 = bootsScriptList.iterator();

            while(var15.hasNext()) {
                BootstrapScript bootsScript = (BootstrapScript)var15.next();
                scriptList.addAll(bootsScript.registerScript());
            }

            List<Class<?>> uniqueScriptList = new ArrayList();
            Iterator var18 = scriptList.iterator();

            while(var18.hasNext()) {
                Class<? extends IScript> script = (Class)var18.next();
                if (uniqueScriptList.contains(script)) {
                    LOGGER.warn("脚本[{}]重复注册", script.getName());
                } else {
                    uniqueScriptList.add(script);
                }
            }

            return checkAndLoad(uniqueScriptList, false);
        } else {
            throw new ScriptException("非调试模式下必须通过-Dgame.script.file参数设置脚本jar包所在路径,设置调试模式请使用-Dgame.script.dev=true");
        }
    }

    private static TwoTuple<JarFile[], URL[]> findJarAndURLFromPaths() {
        URL[] urls = null;
        JarFile[] jarFile = null;
        if (scriptJarFile != null && scriptJarFile.length > 0) {
            jarFile = new JarFile[scriptJarFile.length];
            urls = new URL[scriptJarFile.length];

            for(int i = 0; i < scriptJarFile.length; ++i) {
                String filePath = scriptJarFile[i];
                File file = new File(filePath);
                if (file.isDirectory()) {
                    throw new RuntimeException("脚本文件URL读取失败,原因目标文件是目录,file:" + file.getAbsolutePath());
                }

                if (!file.exists()) {
                    throw new RuntimeException("脚本文件URL读取失败,原因目标文件不存在,file:" + file.getAbsolutePath());
                }

                try {
                    urls[i] = file.toURI().toURL();
                    jarFile[i] = new JarFile(file);
                } catch (IOException var6) {
                    throw new RuntimeException("脚本文件URL读取失败,file:" + file.getAbsolutePath(), var6);
                }
            }
        }

        return new TwoTuple(jarFile, urls);
    }

    private static boolean checkAndLoad(Collection<Class<?>> classList, boolean annotation) throws ScriptException {
        Map<Class<?>, IScript> script_1_to_1 = new HashMap();
        Map<Class<?>, List<IScript>> script_1_to_n = new HashMap();
        Map<IScript, Integer> orderMap = new HashMap();
        Iterator var5 = classList.iterator();

        label68:
        while(true) {
            Class scriptImpl;
            Exclude excludeAnnotation;
            label53:
            do {
                while(var5.hasNext()) {
                    scriptImpl = (Class)var5.next();
                    LOGGER.info("检查脚本类：{}", scriptImpl.getName());
                    if (IScript.class.isAssignableFrom(scriptImpl)) {
                        if (!annotation) {
                            break label53;
                        }

                        excludeAnnotation = (Exclude)scriptImpl.getAnnotation(Exclude.class);
                        continue label53;
                    }

                    LOGGER.error("注册脚本[{}]不是IScript的子类", scriptImpl.getName());
                }

                if (annotation) {
                    script_1_to_n.forEach((k, v) -> {
                        orderMap.getClass();
                        v.sort(Comparator.comparingInt(orderMap::get));
                    });
                }

                ScriptEngine.script_1_to_1 = script_1_to_1;
                ScriptEngine.script_1_to_n = script_1_to_n;
                return true;
            } while(excludeAnnotation != null);

            int modifiers = scriptImpl.getModifiers();
            if (!Modifier.isAbstract(modifiers) && !Modifier.isAbstract(modifiers)) {
                List<Class<?>> scriptIntfList = fetchInterface(scriptImpl);
                if (scriptIntfList.isEmpty()) {
                    LOGGER.error("注册脚本[{}]没有实现任何脚本事件接口", scriptImpl.getName());
                    continue;
                }

                if (!checkField(scriptImpl)) {
                    throw new ScriptException("脚本验证失败");
                }

                try {
                    IScript script = (IScript)scriptImpl.newInstance();
                    if (annotation) {
                        Script scriptAnnotation = (Script)scriptImpl.getAnnotation(Script.class);
                        int order = scriptAnnotation.order();
                        orderMap.put(script, order);
                    }

                    Iterator var16 = scriptIntfList.iterator();

                    while(true) {
                        if (!var16.hasNext()) {
                            continue label68;
                        }

                        Class<?> intf = (Class)var16.next();
                        if (script_1_to_n.containsKey(intf)) {
                            List<IScript> list = (List)script_1_to_n.get(intf);
                            list.add(script);
                            script_1_to_n.put(intf, list);
                        } else if (script_1_to_1.containsKey(intf)) {
                            IScript exist = (IScript)script_1_to_1.remove(intf);
                            List<IScript> list = new ArrayList();
                            list.add(exist);
                            list.add(script);
                            script_1_to_n.put(intf, list);
                        } else {
                            script_1_to_1.put(intf, script);
                        }
                    }
                } catch (Exception var14) {
                    LOGGER.error("检查接口注册失败,script:" + scriptImpl.getName(), var14);
                    return false;
                }
            }

            throw new ScriptException("脚本[{" + scriptImpl.getName() + "}]是一个抽象类或者接口");
        }
    }

    private static List<Class<?>> fetchInterface(Class<?> scriptImpl) {
        Class<?>[] interfaces = scriptImpl.getInterfaces();
        List<Class<?>> ret = new ArrayList();
        Class[] var3 = interfaces;
        int var4 = interfaces.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            Class<?> clazz = var3[var5];
            if (!clazz.equals(IScript.class) && IScript.class.isAssignableFrom(clazz)) {
                ret.add(clazz);
            }
        }

        return ret;
    }

    private static boolean checkField(Class<?> scriptImpl) {
        for(Class clazz = scriptImpl; clazz != Object.class; clazz = clazz.getSuperclass()) {
            Field[] declaredFields = clazz.getDeclaredFields();
            Field[] var3 = declaredFields;
            int var4 = declaredFields.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Field declaredField = var3[var5];
                if (!Modifier.isFinal(declaredField.getModifiers())) {
                    LOGGER.error("脚本中的允许存在的成员变量不是final的,class:[{}], filed:{}", scriptImpl.getName(), declaredField.getName());
                    return false;
                }

                if (!Modifier.isStatic(declaredField.getModifiers())) {
                    LOGGER.error("脚本中的允许存在的成员变量不是static的,class:[{}], filed:{}", scriptImpl.getName(), declaredField.getName());
                    return false;
                }

                if (!Modifier.isPrivate(declaredField.getModifiers())) {
                    LOGGER.error("脚本中的允许存在的成员变量不是private的,class:[{}], filed:{}", scriptImpl.getName(), declaredField.getName());
                    return false;
                }
            }
        }

        return true;
    }

    public static <T extends IScript> T get1t1(Class<T> clazz) {
        IScript ret = (IScript)script_1_to_1.get(clazz);
        if (ret != null) {
            return (T) ret;
        } else {
            List<IScript> list = (List)script_1_to_n.get(clazz);
            if (list != null && !list.isEmpty()) {
                LOGGER.warn("1对N类型的脚本被当做1对1类型来使用，脚本接口：{}", clazz.getName());
                return (T) list.get(0);
            } else {
                return null;
            }
        }
    }

    public static <T extends IScript> List<T> get1tn(Class<T> clazz) {
        List<IScript> ret = (List)script_1_to_n.get(clazz);
        if (ret != null) {
            return (List<T>) ret;
        } else {
            IScript script = (IScript)script_1_to_1.get(clazz);
            if (script != null) {
                LOGGER.warn("1对1类型的脚本被当做1对N类型来使用，脚本接口：{}", clazz.getName());
                ret = new ArrayList(1);
                ret.add(script);
                return (List<T>) ret;
            } else {
                return Collections.emptyList();
            }
        }
    }

    static {
        String scriptPathArrayStr = System.getProperty("game.script.file");
        if (scriptPathArrayStr != null) {
            scriptJarFile = scriptPathArrayStr.split(":");
        }

        String devBoolean = System.getProperty("game.script.dev", "false");
        dev = Boolean.parseBoolean(devBoolean);
        LOGGER.info("当前脚本模式：{},script path:{}", dev, scriptJarFile);
    }
}
