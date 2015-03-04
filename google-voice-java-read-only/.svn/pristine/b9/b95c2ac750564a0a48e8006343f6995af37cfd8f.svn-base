package test.datatypes;

import java.util.ArrayList;
import java.util.List;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.techventus.server.voice.datatypes.EmailAddress;

/**
 * @author Brett Futral @ Catalyst IT Services
 *
 */
public class EmailAddressTest {
	
	String testEmailString = "test@email.com"; 
	String testString;
	
	EmailAddress goldEmail = new EmailAddress("test@email.com");
	EmailAddress testEmailAdd;
		
	List<EmailAddress> testList = new ArrayList<EmailAddress>();
	
	//Generic Phone JSON construction
	JSONObject testJSONPhone = new JSONObject();
	
	//JSON Arrays
	JSONArray times = new JSONArray();
	JSONArray emailAddresses = new JSONArray();
	
	
	
	@Before
	public void setUp() throws JSONException {
		
		emailAddresses.put("testEmailAddress");
		
		testJSONPhone.put("id", 2);
		testJSONPhone.put("name", "testPhone1");
		testJSONPhone.put("phoneNumber", "+15035552121");
		testJSONPhone.put("active", true);
		testJSONPhone.put("behaviorOnRedirect", 1);
		testJSONPhone.put("carrier", "testCarrier");
		testJSONPhone.put("customOverrideState", 1);
		testJSONPhone.put("dEPRECATEDDisabled", false);
		testJSONPhone.put("enabledForOthers", true);
		testJSONPhone.put("formattedNumber", "+15035552121");
		testJSONPhone.put("incomingAccessNumber", "");
		testJSONPhone.put("policyBitmask", 1);
		testJSONPhone.put("redirectToVoicemail", false);
		testJSONPhone.put("scheduleSet", false);
		testJSONPhone.put("smsEnabled", true);
		testJSONPhone.put("telephonyVerified", true);
		testJSONPhone.put("type", 1);
		testJSONPhone.put("verified", true);
		testJSONPhone.put("voicemailForwardingVerified", true);
		testJSONPhone.put("weekdayAllDay", true);
		testJSONPhone.put("weekdayTimes", times);
		testJSONPhone.put("weekendAllDay", true);
		testJSONPhone.put("weekendTimes", times);
		
	}

	@Test
	public void testEmailAddressNoEmail() throws JSONException {

		testEmailAdd = new EmailAddress(testJSONPhone);
		
		Assert.assertEquals("{address=null}", testEmailAdd.toString());
	}
	
	@Test
	public void testEmailAddress() throws JSONException {
		
		testJSONPhone.put("emailAddresses", emailAddresses);

		testEmailAdd = new EmailAddress(testJSONPhone);
		
		Assert.assertEquals("{address=[\"testEmailAddress\"]}", testEmailAdd.toString());
	}
	
	@Test
	public void testCreateEmailAddressListFromJsonPartResponseEmptyString() {
		
		testString = "";
		
		Assert.assertEquals(testList, EmailAddress.createEmailAddressListFromJsonPartResponse(testString));		
		
	}
	
	@Test
	public void testCreateEmailAddressListFromJsonPartResponseNullString() {
		
		List<EmailAddress> nullList = new ArrayList<EmailAddress>();
		
		try {
			nullList = EmailAddress.createEmailAddressListFromJsonPartResponse(testString);
		}
		catch(Exception e) {
			nullList = testList;
		}
		
		Assert.assertEquals(testList, nullList);		
		
	}
	
	@Test
	public void testCreateEmailAddressListFromJsonPartResponse() throws JSONException {
		
		final JSONArray testArray = new JSONArray();
		testArray.put(testEmailString);
		final JSONObject testObject = testArray.toJSONObject(testArray);
		testString = testObject.toString();
		
		testList.add(goldEmail);
				
		Assert.assertEquals(testList.toString(), EmailAddress.createEmailAddressListFromJsonPartResponse(testString).toString());		
		
	}
	
	@Test
	public void testCreateArrayFromJsonObject() throws JSONException {
		
		final JSONArray testArray = new JSONArray();
		testArray.put(testEmailString);
		final JSONObject testObject = new JSONObject();
		testObject.put("emailAddresses", testArray);
		
		final EmailAddress[] testEmailArray = {goldEmail};
		
		Assert.assertEquals(testEmailArray[0].toString(), EmailAddress.createArrayFromJsonObject(testObject)[0].toString());
		
	}

}
