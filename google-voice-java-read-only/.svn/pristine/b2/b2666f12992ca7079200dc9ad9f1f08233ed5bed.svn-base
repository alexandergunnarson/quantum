package test.datatypes;

import junit.framework.Assert;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import org.junit.Before;
import org.junit.Test;

import com.techventus.server.voice.datatypes.Phone;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 * 
 */
public class PhoneTest {
	// testPhones
	final Phone testPhone = new Phone(1, "testPhone", "+15035551212");
	final Phone testPhone1 = new Phone(2, "testPhone1", "+15035552121");
	Phone testPhone2;

	JSONArray times = new JSONArray();

	JSONObject testJSONOb = new JSONObject();
	JSONObject testJSONOb1 = new JSONObject();
	
	final static String RESULT_STRING = "{\"2\":{\"voicemailForwardingVerified\":false,\"weekendAllDay\":false,\"redirectToVoicemail\":false,\"smsEnabled\":false,\"customOverrideState\":0,\"type\":0,\"policyBitmask\":0,\"id\":2,\"phoneNumber\":\"+15035552121\",\"verified\":false,\"dEPRECATEDDisabled\":false,\"name\":\"testPhone1\",\"active\":false,\"enabledForOthers\":false,\"weekdayAllDay\":false,\"telephonyVerified\":false,\"behaviorOnRedirect\":0,\"scheduleSet\":false}}";

	@Before
	public void setUp() throws JSONException {

		testJSONOb1.put("id", 2);
		testJSONOb1.put("name", "testPhone1");
		testJSONOb1.put("phoneNumber", "+15035552121");
		testJSONOb1.put("active", true);
		testJSONOb1.put("behaviorOnRedirect", 1);
		testJSONOb1.put("carrier", "testCarrier");
		testJSONOb1.put("customOverrideState", 1);
		testJSONOb1.put("dEPRECATEDDisabled", false);
		testJSONOb1.put("enabledForOthers", true);
		testJSONOb1.put("formattedNumber", "+15035552121");
		testJSONOb1.put("incomingAccessNumber", "");
		testJSONOb1.put("policyBitmask", 1);
		testJSONOb1.put("redirectToVoicemail", false);
		testJSONOb1.put("scheduleSet", false);
		testJSONOb1.put("smsEnabled", true);
		testJSONOb1.put("telephonyVerified", true);
		testJSONOb1.put("type", 1);
		testJSONOb1.put("verified", true);
		testJSONOb1.put("voicemailForwardingVerified", true);
		testJSONOb1.put("weekdayAllDay", true);
		testJSONOb1.put("weekdayTimes", times);
		testJSONOb1.put("weekendAllDay", true);
		testJSONOb1.put("weekendTimes", times);

	}

	@Test
	public void testPhoneConstructionNull() {

		try {
			testPhone2 = new Phone(testJSONOb);
		} catch (Exception e) {
			testPhone2 = testPhone;
		}
		Assert.assertEquals(testPhone, testPhone2);
	}

	@Test
	public void testPhoneEquality() throws JSONException {

		try {
			testPhone2 = new Phone(testJSONOb1);
		} catch (Exception e) {
			testPhone2 = testPhone;
		}
		
		Assert.assertEquals(testPhone1.getId(), testPhone2.getId());
		Assert.assertEquals(testPhone1.getName(), testPhone2.getName());
		Assert.assertEquals(testPhone1.getPhoneNumber(), testPhone2.getPhoneNumber());
	}


	@Test
	public void testCreateArrayFromJsonObject() throws JSONException {

		final Phone[] resultArray = {testPhone1};
		
		testJSONOb.put("testPhone2", testJSONOb1);
		
		final Phone[] testArray = Phone.createArrayFromJsonObject(testJSONOb);
		
		Assert.assertEquals(resultArray[0].getId(), testArray[0].getId());
		Assert.assertEquals(resultArray[0].getName(), testArray[0].getName());
		Assert.assertEquals(resultArray[0].getPhoneNumber(), testArray[0].getPhoneNumber());
		
	}
	
	@Test
	public void testPhonesArrayToJsonObject() throws JSONException {
		
		final Phone[] testArray = {testPhone1};
		
		final String testString = Phone.phonesArrayToJsonObject(testArray).toString();
		
		Assert.assertEquals(RESULT_STRING, testString);
		
	}

	@Test
	public void testCompareToOverrideEqual() {

		final int test = testPhone.compareTo(testPhone);

		Assert.assertEquals(0, test);
	}

	@Test
	public void testCompareToOverrideGreater() {

		final int test = testPhone1.compareTo(testPhone);

		Assert.assertEquals(1, test);
	}

	@Test
	public void testCompareToOverrideLess() {

		final int test = testPhone.compareTo(testPhone1);

		Assert.assertEquals(-1, test);
	}

}
