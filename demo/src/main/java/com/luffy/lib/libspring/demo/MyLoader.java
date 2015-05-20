package com.luffy.lib.libspring.demo;

import com.luffy.lib.libspring.demo.config.CoreConfig;
import com.luffy.lib.libspring.demo.config.MyRuntimeConfig;
import com.luffy.lib.libspring.demo.config.MyLibSecurityConfig;
import org.luffy.lib.libspring.config.RuntimeConfig;
import org.luffy.lib.libspring.config.LibSecurityConfig;
import org.luffy.lib.libspring.loader.SimpleMVCLoader;

/**
 * Created by CJ on 5/12/15.
 *
 * @author CJ
 */
public class MyLoader extends SimpleMVCLoader{
    @Override
    protected boolean loadDefaultMVC() {
        return true;
    }

    @Override
    public Class<? extends LibSecurityConfig> securityConfig() {
        return MyLibSecurityConfig.class;
    }

    @Override
    protected Class<? extends RuntimeConfig> runtimeConfig() {
        return MyRuntimeConfig.class;
    }

    @Override
    protected Class<?>[] getCoreRootConfigClasses() {
        return new Class<?>[]{
                CoreConfig.class
        };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return null;
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }
}
