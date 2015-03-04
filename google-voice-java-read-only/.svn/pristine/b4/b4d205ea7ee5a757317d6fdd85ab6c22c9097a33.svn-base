package test.datatypes;

import java.util.ArrayList;
import java.util.List;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.techventus.server.voice.datatypes.Greeting;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 *
 */
public class GreetingTest {
	// testGreetings
	Greeting testGreeting = new Greeting(0, "", "System Standard");
	Greeting testGreeting1 = new Greeting(1, "", "testName");
	
	List<Greeting> resultList = new ArrayList<Greeting>();
	
	final static String TEST_STRING = "\"greetings\":[{\"id\":0,\"name\":\"System Standard\",\"jobberName\":\"\"},{\"id\":1,\"name\":\"testName\",\"jobberName\":\"testJobberName\"}],";
	
	JSONObject testJSONObject = new JSONObject();
	
	
	@Before
	public void setUp() {
		resultList.add(testGreeting);
		resultList.add(testGreeting1);
	}

	@Test
	public void testCompareToOverrideEqual() {

		Assert.assertEquals(0, testGreeting.compareTo(testGreeting));
	}

	@Test
	public void testCompareToOverrideGreater() {

		Assert.assertEquals(1, testGreeting1.compareTo(testGreeting));
	}

	@Test
	public void testCompareToOverrideLesser() {

		Assert.assertEquals(-1, testGreeting.compareTo(testGreeting1));
	}

	@Test
	public void testCreateGroupSettingsFromJsonResponse() {
		
		Assert.assertEquals(resultList.toString(), Greeting.createGroupSettingsFromJsonResponse(TEST_STRING).toString());		
	}
	
	@Test
	public void testCreateListFromJsonObjectHasGreetings() throws JSONException {
		
		final JSONObject testJSONGreeting = new JSONObject();
		final JSONObject testJSONGreeting1 = new JSONObject();
		final JSONArray testGreetingArray = new JSONArray();
		testJSONGreeting.put("id", 0);
		testJSONGreeting.put("name", "System Standard");
		testJSONGreeting.put("jobberName", "");
		testJSONGreeting1.put("id", 1);
		testJSONGreeting1.put("name", "testName");
		testJSONGreeting1.put("jobberName", "");
		testGreetingArray.put(testJSONGreeting);
		testGreetingArray.put(testJSONGreeting1);
		testJSONObject.put("greetings", testGreetingArray);
		
		Assert.assertEquals(resultList.toString(), Greeting.createListFromJsonObject(testJSONObject).toString());
		
	}
	
	@Test
	public void testCreateListFromJsonObjectNoGreetings() throws JSONException {
		
		final List<Greeting> noResultList = new ArrayList<Greeting>();
		
		Assert.assertEquals(noResultList.toString(), Greeting.createListFromJsonObject(testJSONObject).toString());
		
	}

}
