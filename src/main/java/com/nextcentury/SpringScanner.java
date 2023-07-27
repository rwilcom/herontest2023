package com.nextcentury;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * 
 * 
 * 
 * 
 * 
 */
@ComponentScan
public class SpringScanner{
    /* nothing to do here - marker class to scan full package */
    static AnnotationConfigApplicationContext ctx;
    
    /**
     * Default/Root configuration scanner only
     * 
     */    
    public static void initializeSpring(){
        initializeSpring( SpringScanner.class );
    }
    
    /**
     * Send in customized configuration scanners
     * 
     * @param configurationScanningBeanClass 
     */
    public static void initializeSpring( Class<?> ... configurationScanningBeanClass ){
        ctx = new AnnotationConfigApplicationContext();
        ctx.register(configurationScanningBeanClass);
        ctx.refresh();
    }
    
    public static ApplicationContext getApplicationContext(){
        if( ctx==null ){
            initializeSpring();
        }
        return ctx;
    }
    
    public static <T> T getBean(Class<T> beanClass){
        
        return getApplicationContext().getBean(beanClass);
    }    
}    