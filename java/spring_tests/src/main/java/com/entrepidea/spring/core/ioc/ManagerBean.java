package com.entrepidea.spring.core.ioc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

//<!--  test how to use depend-on property -->
public class ManagerBean{

	Logger log = LoggerFactory.getLogger(ManagerBean.class);
	
	public ManagerBean(){
		log.info("This is the contructor of {}", ManagerBean.class);
	}
}
