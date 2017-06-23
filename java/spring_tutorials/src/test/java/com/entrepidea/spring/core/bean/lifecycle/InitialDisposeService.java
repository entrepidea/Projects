package com.entrepidea.spring.core.bean.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 *
 * InitialingBean and DisposableBean are among the so called bean lifecycle interfaces.
 *
 * The completed list of the bean lifecycle interfaces can be found from Spring's root interface that access a Spring container: BeanFractory
 * http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/beans/factory/BeanFactory.html
 *
 */
public class InitialDisposeService implements InitializingBean, DisposableBean {

    private static Logger log = LoggerFactory.getLogger(InitialDisposeService.class);

    public void doSomething(){
        log.info("I am Initial dispose service that implements InitilizingBean and DisposableBean.");
    }
    @Override
    public void destroy() throws Exception {
        log.info("I am self-destroyed");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("After properties set.");
    }
}
