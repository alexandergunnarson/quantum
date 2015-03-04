/**
 * 
 */
package test.datatypes;

import junit.framework.Assert;
import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import org.junit.Before;
import org.junit.Test;

import com.techventus.server.voice.datatypes.DisabledId;
import com.techventus.server.voice.datatypes.Group;
import com.techventus.server.voice.datatypes.Setting;

/**
 * @author Brett Futral @ Catalyst IT Services
 * 
 */
public class SettingTest {

	Setting testSetting;
	Setting testSetting1;
	Setting testSetting2;
	Group testGroup;
	String resultString;
	
	final static String DIRECT_CONNECT = "directConnect";
	final static String A_F_IDS = "activeForwardingIds";
	final static String BASE_URL = "baseUrl";
	final static String TEST_URL = "testURL";
	final static String CREDITS = "credits";
	final static String DFID = "defaultGreetingId";
	final static String DID_INFOS = "didInfos";
	final static String DID_MAP = "disabledIdMap";
	final static String D_N_D = "doNotDisturb";
	final static String EMAIL_ADD = "emailAddresses";
	final static String EMAIL_NOTE_ACTIVE = "emailNotificationActive";
	final static String TEST_E_ADDRESS = "testEmailAddress";
	final static String EMAIL_NOTE_ADD = "emailNotificationAddress";
	final static String GREETINGS = "greetings";
	final static String GROUPLIST = "groupList";
	final static String GROUPS = "groups";
	final static String LANGUAGE = "language";
	final static String ENGLISH = "English";
	final static String PRIME_DID = "primaryDid";
	final static String TEST_DID = "testDiD";
	final static String SCREEN_BEHAVE = "screenBehavior";
	final static String SHOW_TRANS = "showTranscripts";
	final static String SMS_NOTE = "smsNotifications";
	final static String SMS_EMAIL_ACTIVE = "smsToEmailActive";
	final static String SMS_EMAIL_SUB = "smsToEmailSubject";
	final static String SPAM = "spam";
	final static String TIME_ZONE = "timezone";
	final static String TEST_T_ZONE = "testTimeZone";
	final static String USE_DID_CID = "useDidAsCallerId";
	final static String USE_DID_SOURCE = "useDidAsSource";
	
	final JSONArray emptyJSONArray = new JSONArray();
	JSONArray testGroupList = new JSONArray();
	JSONArray testGroupArray = new JSONArray();	

	final JSONObject emptyJSONObject = new JSONObject();
	final static JSONObject TEST_NULL_OBJECT = null;
	JSONObject testJSONGroupOb = new JSONObject();
	JSONObject testJSONGroupOb1 = new JSONObject();
	JSONObject jsonPreSetting = new JSONObject();
	JSONObject jsonSetting = new JSONObject();
	JSONObject testJSONObject = new JSONObject();

	final DisabledId testDisabledID = new DisabledId("3", true);
	final DisabledId testDisabledID1 = new DisabledId("4", true);

	DisabledId[] testDisArray = { testDisabledID, testDisabledID1 };

	@Before
	public void setUp() throws Exception {

		// Build groups
		testJSONGroupOb.put("id", "testID");
		testJSONGroupOb.put("name", "testName");
		testJSONGroupOb.put("isCustomForwarding", false);
		testJSONGroupOb.put("isCustomGreeting", false);
		testJSONGroupOb.put("isCustomDirectConnect", false);
		testJSONGroupOb.put(DIRECT_CONNECT, false);
		testJSONGroupOb.put("greetingId", 1);
		testJSONGroupOb.put("disabledForwardingIds", emptyJSONObject);
		testGroup = new Group(testJSONGroupOb);
		// Insert testGroup into an array
		testGroupArray.put(testGroup);
		// And move it into a new Object
		testJSONGroupOb1.put("testID", testJSONGroupOb);
		
		testGroupList.put("testID");
		testGroupList.put("testID1");
		
		jsonPreSetting.put(A_F_IDS, emptyJSONArray);
		jsonPreSetting.put(BASE_URL, TEST_URL);
		jsonPreSetting.put(CREDITS, 1);
		jsonPreSetting.put(DFID, 1);
		jsonPreSetting.put(DID_INFOS, emptyJSONArray);
		jsonPreSetting.put(DIRECT_CONNECT, true);
		jsonPreSetting.put(DID_MAP, emptyJSONObject);
		jsonPreSetting.put(D_N_D, false);
		jsonPreSetting.put(EMAIL_ADD, emptyJSONObject);
		jsonPreSetting.put(EMAIL_NOTE_ACTIVE, true);
		jsonPreSetting.put(EMAIL_NOTE_ADD, TEST_E_ADDRESS);
		jsonPreSetting.put(GREETINGS, emptyJSONObject);
		jsonPreSetting.put(GROUPLIST, testGroupList);
		jsonPreSetting.put(GROUPS, testJSONGroupOb1);
		jsonPreSetting.put(LANGUAGE, ENGLISH);
		jsonPreSetting.put(PRIME_DID, TEST_DID);
		jsonPreSetting.put(SCREEN_BEHAVE, 1);
		jsonPreSetting.put(SHOW_TRANS, false);
		jsonPreSetting.put(SMS_NOTE, emptyJSONArray);
		jsonPreSetting.put(SMS_EMAIL_ACTIVE, true);
		jsonPreSetting.put(SMS_EMAIL_SUB, false);
		jsonPreSetting.put(SPAM, SPAM);
		jsonPreSetting.put(TIME_ZONE, TEST_T_ZONE);
		jsonPreSetting.put(USE_DID_CID, true);
		jsonPreSetting.put(USE_DID_SOURCE, false);
		testSetting = new Setting(jsonPreSetting);
		testSetting.setmDisabledIdList(null);

	}

	/**
	 * Test method for
	 * {@link com.techventus.server.voice.datatypes.Setting#Setting(gvjava.org.json.JSONObject)}
	 * .
	 * 
	 * @throws JSONException
	 */
	@Test
	public void testSettingNullObject() throws JSONException {

		testSetting = new Setting(testJSONObject);

		Assert.assertNull(testSetting.getmActiveForwardingList());

	}
	
	@Test
	public void testSettings() throws JSONException {

		Assert.assertEquals(1,testSetting.getCredits());

	}

	/**
	 * Test method for
	 * {@link com.techventus.server.voice.datatypes.Setting#toJsonObject()}.
	 * @throws JSONException 
	 */
	@Test
	public void testToJsonObjectNullEmailAndDiD() throws JSONException {

		jsonPreSetting.put(A_F_IDS, emptyJSONArray);
		jsonPreSetting.put(BASE_URL, TEST_URL);
		jsonPreSetting.put(CREDITS, 1);
		jsonPreSetting.put(DFID, 1);
		jsonPreSetting.put(DID_INFOS, emptyJSONArray);
		jsonPreSetting.put(DIRECT_CONNECT, true);
		jsonPreSetting.put(DID_MAP, emptyJSONObject);
		jsonPreSetting.put(D_N_D, false);
		jsonPreSetting.put(EMAIL_ADD, TEST_NULL_OBJECT);
		jsonPreSetting.put(EMAIL_NOTE_ACTIVE, true);
		jsonPreSetting.put(EMAIL_NOTE_ADD, TEST_E_ADDRESS);
		jsonPreSetting.put(GREETINGS, emptyJSONObject);
		jsonPreSetting.put(GROUPLIST, testGroupList);
		jsonPreSetting.put(GROUPS, testJSONGroupOb1);
		jsonPreSetting.put(LANGUAGE, ENGLISH);
		jsonPreSetting.put(PRIME_DID, TEST_DID);
		jsonPreSetting.put(SCREEN_BEHAVE, 1);
		jsonPreSetting.put(SHOW_TRANS, false);
		jsonPreSetting.put(SMS_NOTE, emptyJSONArray);
		jsonPreSetting.put(SMS_EMAIL_ACTIVE, true);
		jsonPreSetting.put(SMS_EMAIL_SUB, false);
		jsonPreSetting.put(SPAM, SPAM);
		jsonPreSetting.put(TIME_ZONE, TEST_T_ZONE);
		jsonPreSetting.put(USE_DID_CID, true);
		jsonPreSetting.put(USE_DID_SOURCE, false);
		
		testSetting = new Setting(jsonPreSetting);
		testSetting.setmDisabledIdList(null);
		
		testJSONObject = testSetting.toJsonObject();
		
		resultString = "{\"primaryDid\":\"testDiD\",\"defaultGreetingId\":1,\"useDidAsSource\":false,\"doNotDisturb\":false,\"smsNotifications\":[],\"emailNotificationActive\":true,\"credits\":1,\"activeForwardingIds\":[],\"emailNotificationAddress\":\"testEmailAddress\",\"smsToEmailSubject\":false,\"baseUrl\":\"testURL\",\"timezone\":\"testTimeZone\",\"groupList\":[\"testID\",\"testID1\"],\"didInfos\":[],\"greetings\":[],\"showTranscripts\":false,\"directConnect\":true,\"screenBehavior\":1,\"language\":\"English\",\"spam\":\"spam\",\"useDidAsCallerId\":true,\"smsToEmailActive\":true,\"groups\":{\"testID\":{\"id\":\"testID\",\"greetingId\":1,\"isCustomForwarding\":false,\"isCustomGreeting\":false,\"disabledForwardingIds\":{},\"name\":\"testName\",\"isCustomDirectConnect\":false,\"directConnect\":false}}}";

		Assert.assertEquals(resultString, testJSONObject.toString());
		
		}
	
	/**
	 * Test method for
	 * {@link com.techventus.server.voice.datatypes.Setting#toJsonObject()}.
	 * @throws JSONException 
	 */
	@Test
	public void testToJsonObject() throws JSONException {
		
		final JSONArray testEmailArray = new JSONArray();
		testEmailArray.put("test@email.com");
		
		final JSONObject testDiDObject = new JSONObject();
		testDiDObject.put("1", true);

		jsonPreSetting.put(A_F_IDS, emptyJSONArray);
		jsonPreSetting.put(BASE_URL, TEST_URL);
		jsonPreSetting.put(CREDITS, 1);
		jsonPreSetting.put(DFID, 1);
		jsonPreSetting.put(DID_INFOS, emptyJSONArray);
		jsonPreSetting.put(DIRECT_CONNECT, true);
		jsonPreSetting.put(DID_MAP, testDiDObject);
		jsonPreSetting.put(D_N_D, false);
		jsonPreSetting.put(EMAIL_ADD, testEmailArray);
		jsonPreSetting.put(EMAIL_NOTE_ACTIVE, true);
		jsonPreSetting.put(EMAIL_NOTE_ADD, TEST_E_ADDRESS);
		jsonPreSetting.put(GREETINGS, emptyJSONObject);
		jsonPreSetting.put(GROUPLIST, testGroupList);
		jsonPreSetting.put(GROUPS, testJSONGroupOb1);
		jsonPreSetting.put(LANGUAGE, ENGLISH);
		jsonPreSetting.put(PRIME_DID, TEST_DID);
		jsonPreSetting.put(SCREEN_BEHAVE, 1);
		jsonPreSetting.put(SHOW_TRANS, false);
		jsonPreSetting.put(SMS_NOTE, emptyJSONArray);
		jsonPreSetting.put(SMS_EMAIL_ACTIVE, true);
		jsonPreSetting.put(SMS_EMAIL_SUB, false);
		jsonPreSetting.put(SPAM, SPAM);
		jsonPreSetting.put(TIME_ZONE, TEST_T_ZONE);
		jsonPreSetting.put(USE_DID_CID, true);
		jsonPreSetting.put(USE_DID_SOURCE, false);
		
		testSetting = new Setting(jsonPreSetting);
		
		testJSONObject = testSetting.toJsonObject();
		
		resultString = "{\"doNotDisturb\":false,\"disabledIdMap\":{\"1\":true},\"smsNotifications\":[],\"credits\":1,\"baseUrl\":\"testURL\",\"groupList\":[\"testID\",\"testID1\"],\"timezone\":\"testTimeZone\",\"greetings\":[],\"showTranscripts\":false,\"useDidAsCallerId\":true,\"groups\":{\"testID\":{\"id\":\"testID\",\"greetingId\":1,\"isCustomForwarding\":false,\"isCustomGreeting\":false,\"disabledForwardingIds\":{},\"name\":\"testName\",\"isCustomDirectConnect\":false,\"directConnect\":false}},\"primaryDid\":\"testDiD\",\"defaultGreetingId\":1,\"useDidAsSource\":false,\"emailNotificationActive\":true,\"emailNotificationAddress\":\"testEmailAddress\",\"activeForwardingIds\":[],\"smsToEmailSubject\":false,\"didInfos\":[],\"directConnect\":true,\"screenBehavior\":1,\"language\":\"English\",\"spam\":\"spam\",\"smsToEmailActive\":true,\"emailAddresses\":\"test@email.com\"}";

		Assert.assertEquals(resultString, testJSONObject.toString());
		
		}

	/**
	 * Test method for
	 * {@link com.techventus.server.voice.datatypes.Setting#getGroupListAsList()}
	 * .
	 * @throws JSONException 
	 */
	@Test
	public void testGetGroupListAsList() throws JSONException {
		
		final JSONArray testEmailArray = new JSONArray();
		testEmailArray.put("test@email.com");
		
		final JSONObject testDiDObject = new JSONObject();
		testDiDObject.put("1", true);

		jsonPreSetting.put(A_F_IDS, emptyJSONArray);
		jsonPreSetting.put(BASE_URL, TEST_URL);
		jsonPreSetting.put(CREDITS, 1);
		jsonPreSetting.put(DFID, 1);
		jsonPreSetting.put(DID_INFOS, emptyJSONArray);
		jsonPreSetting.put(DIRECT_CONNECT, true);
		jsonPreSetting.put(DID_MAP, testDiDObject);
		jsonPreSetting.put(D_N_D, false);
		jsonPreSetting.put(EMAIL_ADD, testEmailArray);
		jsonPreSetting.put(EMAIL_NOTE_ACTIVE, true);
		jsonPreSetting.put(EMAIL_NOTE_ADD, TEST_E_ADDRESS);
		jsonPreSetting.put(GREETINGS, emptyJSONObject);
		jsonPreSetting.put(GROUPLIST, testGroupList);
		jsonPreSetting.put(GROUPS, testJSONGroupOb1);
		jsonPreSetting.put(LANGUAGE, ENGLISH);
		jsonPreSetting.put(PRIME_DID, TEST_DID);
		jsonPreSetting.put(SCREEN_BEHAVE, 1);
		jsonPreSetting.put(SHOW_TRANS, false);
		jsonPreSetting.put(SMS_NOTE, emptyJSONArray);
		jsonPreSetting.put(SMS_EMAIL_ACTIVE, true);
		jsonPreSetting.put(SMS_EMAIL_SUB, false);
		jsonPreSetting.put(SPAM, SPAM);
		jsonPreSetting.put(TIME_ZONE, TEST_T_ZONE);
		jsonPreSetting.put(USE_DID_CID, true);
		jsonPreSetting.put(USE_DID_SOURCE, false);
		
		testSetting = new Setting(jsonPreSetting);
		
		resultString = "[testID, testID1]";
		
		Assert.assertEquals(resultString, testSetting.getGroupListAsList().toString());
	}

}
