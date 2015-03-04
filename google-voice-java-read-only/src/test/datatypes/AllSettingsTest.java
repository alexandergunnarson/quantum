package test.datatypes;

import java.util.ArrayList;
import java.util.List;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.techventus.server.voice.datatypes.AllSettings;
import com.techventus.server.voice.datatypes.DisabledId;
import com.techventus.server.voice.datatypes.Greeting;
import com.techventus.server.voice.datatypes.Group;
import com.techventus.server.voice.datatypes.Phone;
import com.techventus.server.voice.datatypes.Setting;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 * 
 */
public class AllSettingsTest {
	// testAllSettings(s)
	AllSettings testAllSettings;
	AllSettings testAllSettings1;
	// Params for testAllSettings(s)
	Setting testSetting;
	Greeting testGreeting;
	Group testGroup;
	Phone testPhone;
	Phone testPhone1;
	Phone testPhone2;

	
	final JSONObject emptyJSONObject = new JSONObject();
	JSONObject testJSONGroupOb = new JSONObject();
	JSONObject testJSONGroupOb1 = new JSONObject();
	JSONObject jsonSetting = new JSONObject();
	JSONObject testJSONPhoneOb = new JSONObject();
	JSONObject jsonPhone = new JSONObject();
	
	final JSONArray emptyJSONArray = new JSONArray();
	JSONArray testGroupList = new JSONArray();
	JSONArray testGroupArray = new JSONArray();
	JSONArray testPhoneList = new JSONArray();
	
	final DisabledId testDisabledID = new DisabledId("3", true);
	final DisabledId testDisabledID1 = new DisabledId("4", true);
	
	DisabledId[] testDisArray = { testDisabledID, testDisabledID1 };
	
	private final static String NAME = "name";

	@Before
	public void setUp() throws Exception {

		// Build groupList
		
		testGroupList.put("testID");

		// Build groups
		testJSONGroupOb.put("id", "testID");
		testJSONGroupOb.put(NAME, "testName");
		testJSONGroupOb.put("isCustomForwarding", false);
		testJSONGroupOb.put("isCustomGreeting", false);
		testJSONGroupOb.put("isCustomDirectConnect", false);
		testJSONGroupOb.put("directConnect", false);
		testJSONGroupOb.put("greetingId", 1);
		testJSONGroupOb.put("disabledForwardingIds", emptyJSONObject);
		testGroup = new Group(testJSONGroupOb);
		// Insert testGroup into an array
		testGroupArray.put(testGroup);
		// And move it into a new Object
		testJSONGroupOb1.put("testID", testJSONGroupOb);

		// Build Setting
		jsonSetting.put("activeForwardingIds", emptyJSONArray);
		jsonSetting.put("baseUrl", "testURL");
		jsonSetting.put("credits", 1);
		jsonSetting.put("defaultGreetingId", 1);
		jsonSetting.put("didInfos", emptyJSONArray);
		jsonSetting.put("directConnect", true);
		jsonSetting.put("disabledIdMap", emptyJSONObject);
		jsonSetting.put("doNotDisturb", false);
		jsonSetting.put("emailAddresses", emptyJSONObject);
		jsonSetting.put("emailNotificationActive", true);
		jsonSetting.put("emailNotificationAddress", "testEmailAddress");
		jsonSetting.put("greetings", emptyJSONObject);
		jsonSetting.put("groupList", testGroupList);
		jsonSetting.put("groups", testJSONGroupOb1);
		jsonSetting.put("language", "English");
		jsonSetting.put("primaryDid", "testDiD");
		jsonSetting.put("screenBehavior", 1);
		jsonSetting.put("showTranscripts", false);
		jsonSetting.put("smsNotifications", emptyJSONArray);
		jsonSetting.put("smsToEmailActive", true);
		jsonSetting.put("smsToEmailSubject", false);
		jsonSetting.put("spam", "spam");
		jsonSetting.put("timezone", "testTimeZone");
		jsonSetting.put("useDidAsCallerId", true);
		jsonSetting.put("useDidAsSource", false);
		testSetting = new Setting(jsonSetting);
		testSetting.setmDisabledIdList(null);
		final JSONObject testJSONSetting = testSetting.toJsonObject();
		// AddDisabledIDs
		testSetting.setmDisabledIdList(testDisArray);
		final JSONObject testJSONSetting1 = testSetting.toJsonObject();

		// Build PhoneList
		testPhoneList.put(1);
		testPhoneList.put(2);
		testPhoneList.put(3);

		// Build Phones
		// Build testPhone
		testJSONPhoneOb.put("id", 1);
		testJSONPhoneOb.put(NAME, "testPhone");
		testJSONPhoneOb.put("phoneNumber", "+15035552121");
		testJSONPhoneOb.put("active", true);
		testJSONPhoneOb.put("behaviorOnRedirect", 1);
		testJSONPhoneOb.put("carrier", "testCarrier");
		testJSONPhoneOb.put("customOverrideState", 1);
		testJSONPhoneOb.put("dEPRECATEDDisabled", false);
		testJSONPhoneOb.put("enabledForOthers", true);
		testJSONPhoneOb.put("formattedNumber", "+15035552121");
		testJSONPhoneOb.put("incomingAccessNumber", "");
		testJSONPhoneOb.put("policyBitmask", 1);
		testJSONPhoneOb.put("redirectToVoicemail", false);
		testJSONPhoneOb.put("scheduleSet", false);
		testJSONPhoneOb.put("smsEnabled", true);
		testJSONPhoneOb.put("telephonyVerified", true);
		testJSONPhoneOb.put("type", 1);
		testJSONPhoneOb.put("verified", true);
		testJSONPhoneOb.put("voicemailForwardingVerified", true);
		testJSONPhoneOb.put("weekdayAllDay", true);
		testJSONPhoneOb.put("weekdayTimes", emptyJSONArray);
		testJSONPhoneOb.put("weekendAllDay", true);
		testJSONPhoneOb.put("weekendTimes", emptyJSONArray);
		// testPhone
		testPhone = new Phone(testJSONPhoneOb);
		final JSONObject testJSONPhone = testPhone.toJsonObject();
		// Build testPhone1
		testJSONPhoneOb.put("id", 2);
		testJSONPhoneOb.put(NAME, "testPhone1");
		testJSONPhoneOb.put("phoneNumber", "+15035551212");
		testJSONPhoneOb.put("formattedNumber", "+15035551212");
		// testPhone
		testPhone1 = new Phone(testJSONPhoneOb);
		final JSONObject testJSONPhone1 = testPhone1.toJsonObject();
		// Build testPhone2
		testJSONPhoneOb.put("id", 3);
		testJSONPhoneOb.put(NAME, "testPhone2");
		testJSONPhoneOb.put("phoneNumber", "+15035552123");
		testJSONPhoneOb.put("formattedNumber", "+15035552123");
		// testPhone
		testPhone2 = new Phone(testJSONPhoneOb);
		final JSONObject testJSONPhone2 = testPhone2.toJsonObject();
		// And put them in an Object
		jsonPhone.put("1", testJSONPhone);
		jsonPhone.put("2", testJSONPhone1);
		jsonPhone.put("3", testJSONPhone2);

		// BuildAllSettings
		final JSONObject jsAllSettings = new JSONObject();
		jsAllSettings.put("phoneList", testPhoneList);
		jsAllSettings.put("phones", jsonPhone);
		jsAllSettings.put("settings", testJSONSetting);

		// BuildAllSettings1
		final JSONObject jsAllSettings1 = new JSONObject();
		jsAllSettings1.put("phoneList", testPhoneList);
		jsAllSettings1.put("phones", jsonPhone);
		jsAllSettings1.put("settings", testJSONSetting1);

		testAllSettings = new AllSettings(jsAllSettings.toString());
		testAllSettings1 = new AllSettings(jsAllSettings1.toString());

	}

	@Test
	public void testIsPhoneDisabledNullList() throws JSONException {

		final boolean test = testAllSettings.isPhoneDisabled(1);

		Assert.assertEquals(false, test);

	}

	@Test
	public void testIsPhoneDisabledFalse() throws JSONException {

		final boolean test = testAllSettings1.isPhoneDisabled(2);

		Assert.assertEquals(false, test);
	}

	@Test
	public void testIsPhoneDisabledTrue() throws JSONException {

		final boolean test = testAllSettings1.isPhoneDisabled(3);

		Assert.assertEquals(true, test);
	}

	@Test
	public void testGetPhoneListAsList() {

		final List<Integer> testList = new ArrayList<Integer>();
		testList.add(1);
		testList.add(2);
		testList.add(3);

		Assert.assertEquals(testList, testAllSettings.getPhoneListAsList());

	}

}
