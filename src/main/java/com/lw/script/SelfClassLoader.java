package com.lw.script;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.ProtectionDomain;

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
 *  @data 2020/10/16 10:36
 *  @author lw
 */
public class SelfClassLoader extends ClassLoader {
    private String classPath;

    public static void main(String[] args) throws ClassNotFoundException {
        SelfClassLoader scriptClassLoader = new SelfClassLoader("F:\\Server\\Branch\\game-logic\\target\\classes");
        Class<?> c = scriptClassLoader.loadClass("com.sh.logic.system.GameLoadScript");
        System.out.println(c.getClassLoader());
    }

    public SelfClassLoader(String classPath) {
        this.classPath = classPath;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] data = this.readClassFile(name);
        return data == null ? null : this.defineClass(name, data, 0, data.length, (ProtectionDomain)null);
    }

    private byte[] readClassFile(String name) {
        byte[] data = null;

        try {
            File file = new File(this.classPath + this.classNameToFilePath(name));
            System.out.println(file.getAbsolutePath());
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int ch;
            while((ch = fis.read()) != -1) {
                out.write(ch);
            }

            data = out.toByteArray();
        } catch (IOException var7) {
            var7.printStackTrace();
            System.out.println("readClassFile-IOException");
        }

        return data;
    }

    private String classNameToFilePath(String className) {
        String[] array = className.split(".");
        StringBuilder builder = new StringBuilder();
        String[] var4 = array;
        int var5 = array.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            String str = var4[var6];
            builder.append("/").append(str);
        }

        builder.append(".class");
        return builder.toString();
    }
}
