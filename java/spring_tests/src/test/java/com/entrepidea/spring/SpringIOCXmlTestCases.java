package com.entrepidea.spring;

import static org.junit.Assert.assertNotSame;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.entrepidea.spring.core.ioc.ClientService;
import com.entrepidea.spring.core.ioc.CommandManager;
import com.entrepidea.spring.core.ioc.ComplexObject;
import com.entrepidea.spring.core.ioc.Person;
import com.entrepidea.spring.core.ioc.PrototypeClient;
import com.entrepidea.spring.core.ioc.SimplestBean;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/config/META-INF/spring/core-config-context.xml" })
public class SpringIOCXmlTestCases {

	private static Logger LOGGER = LoggerFactory.getLogger(SpringIOCXmlTestCases.class);

	//this is the easiest way to retrieve the application context.
	@Autowired
	private ApplicationContext ac;
	
	@Autowired
	private SimplestBean simplestBean;
	
	@Test
	public void testExampleBean(){
		String name = simplestBean.myName();
		LOGGER.info("my name is {} ", name);
	}
	
	@Test
	public void testExampleBean2(){
		SimplestBean sb = (SimplestBean)ac.getBean("simplestBean");
		LOGGER.info("my name is {} ", sb.myName());
	}
	
	@Test
	public void testStaticMethodInstantilization(){
		ClientService clientService = ac.getBean(ClientService.class);
		LOGGER.info("Testing Spring to instantialize a bean through static factory method - {} ",clientService.getServiceType());
	}
	
	@Test
	public void testMergeCollection(){
		ComplexObject co = (ComplexObject)ac.getBean("child");
		Properties prop = co.getAdminEmails();
		
		Set<Entry<Object, Object>> s = prop.entrySet();
		for(Iterator<Entry<Object, Object>> iter = s.iterator(); iter.hasNext();){
			Entry<Object, Object> ele = iter.next();
			LOGGER.debug("name={},value={}", ele.getKey(), ele.getValue());
		}
		
	}
	
	
	
	//<!--  example of using p-namespace. It needs to be declared but not need to specify the xsd; -->
	//<!-- look at the special ref: spouse-ref, it's the property + ref -->
	@Test
	public void testPNameSpace(){
		Person john = (Person)ac.getBean("john-modern");
		Person wife = john.getSpouse();
		LOGGER.info("John's wife is {}", wife.getName());
	}
	@Autowired
	@Qualifier("john-modern")
	private Person john;
	@Test
	public void testPNameSpace2(){
		Person wife = john.getSpouse();
		LOGGER.info("John's wife is {}", wife.getName());
	}
	
	
	//<!--  test how to use depend-on property -->
	@Test
	public void testDependonProperty(){
		ac.getBean("dependOnBean");
	}
	
	@Autowired
	private CommandManager commandManager;
	@Test
	public void testMethodInjection(){
		//this is a test of method injection, since the Command is defined as a "prototype" scope in configuration, the below two will return
		//different instances. Try changing the scope to "singleton" to see what will happen.
		commandManager.getCommand();
		commandManager.getCommand();
	}
	
	
	
	
	//<!--  example of bean with prototype scope -->
	@Autowired
	private PrototypeClient pc;
	@Test
	public void testPrototypeBean(){
		LOGGER.info("bean1 ===> {} ", pc.getBean1());
		LOGGER.info("bean2 ===> {} ", pc.getBean2());
		LOGGER.info("bean3 ===> {} ", pc.getBean3());
		assertNotSame("bean1 is not the same as bean2",pc.getBean1(),pc.getBean2() );
		assertNotSame("bean2 is not the same as bean3",pc.getBean2(),pc.getBean3() );
		assertNotSame("bean1 is not the same as bean3",pc.getBean1(),pc.getBean3() );
		
	}
	
	
	

	
	//Spring's environment abstraction covers system.getproperty and system.getenv
	@Test
	public void testPropertySource(){
		System.setProperty("foo", "val");
		ApplicationContext ctx = new GenericApplicationContext();
		Environment env = ctx.getEnvironment();
		Assert.assertTrue(env.containsProperty("foo"));
	}
	//<!-- this is supposed to be used for a MessageResource test -->
	//Spring ApplicationContext's additional functionalities. MessageSource for I18N
	//failed to work on 05/24/15 - fix it!
	@Test
	public void testMessageSource(){
		MessageSource msgSrc = new ClassPathXmlApplicationContext("/META-INF/spring/messageresource.xml");
		String msg = msgSrc.getMessage("message", null, "Default", null);
		LOGGER.info("message is {}",msg);
		
		Assert.assertEquals("Alligators rock!", msg);
	}	
}
