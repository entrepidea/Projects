package com.entrepidea.spring.core.support;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Component;

/**
 * Created by jonat on 4/22/2017.
 */
@Component
public class SingletonBean {


    public void showMe(){
        createPrototypeBean().showMe();
    }

    @Lookup
    public PrototypeBean createPrototypeBean(){
        return new PrototypeBean();
    }
}
