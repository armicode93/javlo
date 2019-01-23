package org.javlo.helper;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class BeanHelperTest extends TestCase {

	public BeanHelperTest() {
		// TODO Auto-generated constructor stub
	}

	public BeanHelperTest(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	private static class Bean {
		private String firstname;
		private String lastname;
		private String title;
		private int age;
		private long dist;

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

		public String getLastname() {
			return lastname;
		}

		public void setLastname(String lastname) {
			this.lastname = lastname;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public long getDist() {
			return dist;
		}

		public void setDist(long dist) {
			this.dist = dist;
		}
	}

	public void testSetProperty() throws Exception {
		Bean bean = new Bean();
		BeanHelper.setProperty(bean, "firstname", "Patrick");
		BeanHelper.setProperty(bean, "lastname", "MyName");
		assertEquals(bean.getFirstname(), "Patrick");
		assertEquals(bean.getLastname(), "MyName");
		bean.setLastname("noname");
		BeanHelper.setProperty(bean, "lastname", null);
		assertEquals(bean.getLastname(), null);
	}
	
	public void testCopy() throws Exception {
		Map<String,String> test = new HashMap<>();
		test.put("firstname", "Barbara");

		Bean b = new Bean();
//		BeanHelper.copy(test, b);
//		assertEquals(b.getFirstname(), "Barbara");
		test.put("firstname-69", "Catherine");
		test.put("age-69", "23");
		test.put("dist-69", "9999");
		BeanHelper.copy(test, b, null, "-69");
		assertEquals(b.getFirstname(), "Catherine");
		assertEquals(b.getAge(), 23);
		assertEquals(b.getDist(), 9999);
		
//		test = new HashMap<>();
//		test.put("*firstname-69", "Patrick");
//		BeanHelper.copy(test, b, "*", "-69");
//		assertEquals(b.getFirstname(), "Patrick");
//
//		b.setFirstname("test");
//		test.put("*firstname-69", "Patrick");
//		BeanHelper.copy(test, b, "+", "-69");
//		assertEquals(b.getFirstname(), "test");
//		
//		
//		Bean a = new Bean();
//		test.put("firstname-69", "Isabelle");
//		BeanHelper.copy(test, a, null, "-69");
	}
}
