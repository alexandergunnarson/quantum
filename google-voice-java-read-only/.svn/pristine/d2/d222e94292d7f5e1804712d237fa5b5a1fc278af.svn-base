package test.datatypes;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.techventus.server.voice.datatypes.DisabledForwardingId;
import com.techventus.server.voice.datatypes.Group;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 *
 */
public class GroupTest {
	// testGroups
	Group testGroup;
	Group testGroup1;
	Group testGroup2;
	Group testGroup3;
	
	String testString;
	String resultString;
	private final static String TEST_ID = "testID";

	// DisabledForwardingIDs
	final DisabledForwardingId testDFId = new DisabledForwardingId(
			"2", true);
	final DisabledForwardingId testDFId1 = new DisabledForwardingId(
			"3", true);

	List<DisabledForwardingId> testList = new ArrayList<DisabledForwardingId>();
	List<DisabledForwardingId> testList1 = new ArrayList<DisabledForwardingId>();
	List<Group> resultList = new ArrayList<Group>();
	List<Group> testGrouplist = new ArrayList<Group>();

	JSONArray testJSArray1 = new JSONArray();
	JSONObject disabledFIDs = new JSONObject();
	JSONObject testJSONOb1 = new JSONObject();
	JSONObject testJSONOb3 = new JSONObject();
	JSONObject testGroupObject = new JSONObject();

	@Before
	public void setUp() throws Exception {

		// Populate DFID List1
		testList1.add(testDFId1);
		testList1.add(testDFId);

		// JSON Array to attempt to mirror populated list.
		testJSArray1.put(3);
		testJSArray1.put(2);

		// New JSON objects to mirror lists above.
		final JSONObject disabledFIDs1 = testJSArray1.toJSONObject(testJSArray1);

		// Construct Object 1
		testJSONOb1.put("id", TEST_ID);
		testJSONOb1.put("name", "testName");
		testJSONOb1.put("isCustomForwarding", false);
		testJSONOb1.put("isCustomGreeting", false);
		testJSONOb1.put("isCustomDirectConnect", false);
		testJSONOb1.put("directConnect", false);
		testJSONOb1.put("greetingId", 0);
		testJSONOb1.put("disabledForwardingIds", disabledFIDs);
		
		// Construct Object 3
		testJSONOb3.put("id", "testID1");
		testJSONOb3.put("name", "testName1");
		testJSONOb3.put("isCustomForwarding", false);
		testJSONOb3.put("isCustomGreeting", false);
		testJSONOb3.put("isCustomDirectConnect", false);
		testJSONOb3.put("directConnect", false);
		testJSONOb3.put("greetingId", 0);
		testJSONOb3.put("disabledForwardingIds", disabledFIDs1);

		// Instantiate testGroups
		testGroup = new Group(TEST_ID, "testName", false, testList, false,
				false, false, 0);
		testGroup1 = new Group(testJSONOb1);
		testGroup2 = new Group("testID1", "testName1", false, testList1, false,
				false, false, 0);
		testGroup3 = new Group(testJSONOb3);

	}

	@Test
	public void testJSONNullGroupObject() throws JSONException {

		
		final Group nullGroup = new Group(testGroupObject);
		
		resultString = "{id=null;name=null;isCustomDirectConnect=false;directConnect=false;isCustomGreeting=false;isCustomForwarding=false;greetingId=0;disabledForwardingIds=null}";

		Assert.assertEquals(resultString, nullGroup.toString());
		
	}

	@Test
	public void testJSONGroupObjectEqualsJavaGroupObject() {

		final boolean test = testGroup.toString().equals(testGroup1.toString());

		Assert.assertEquals(true, test);
	}
	
	@Test
	public void testCreateGroupSettingsFromJsonResponse() {
		
		resultList.add(testGroup);
		
		testString = "\"groups\":{\"testID\":{\"id\":\"testID\",\"name\":\"testName\",\"disabledForwardingIds\":{1},\"isCustomForwarding\":false,\"isCustomGreeting\":false,\"isCustomDirectConnect\":false,\"directConnect\":false,\"greetingId\":0,\"isCircle\":true,\"isCustomTranscriptionLanguage\":false,\"transcriptionLanguage\":\"\"},\"groupList\"";
		
		testGrouplist = Group.createGroupSettingsFromJsonResponse(testString);
		
		Assert.assertEquals(resultList.toString(), testGrouplist.toString());
	}
	
	@Test
	public void testCreateGroupSettingsFromJsonResponseNullDisabledForwardingID() {
		
		testString = "\"groups\":{\"testID\":{\"id\":\"testID\",\"name\":\"testName\",\"isCustomForwarding\":false,\"isCustomGreeting\":false,\"isCustomDirectConnect\":false,\"directConnect\":false,\"greetingId\":0,\"isCircle\":true,\"isCustomTranscriptionLanguage\":false,\"transcriptionLanguage\":\"\"},\"groupList\"";
		
		try {
		testGrouplist = Group.createGroupSettingsFromJsonResponse(testString);
		}
		catch(Exception e) {
			testGrouplist = resultList;
		}
		Assert.assertEquals(resultList, testGrouplist);
	}
	
	@Test
	public void testCreateGroupSettingsFromJsonResponseEmptyDisabledForwardingID() {
		
		resultList.add(testGroup);
		
		testString = "\"groups\":{\"testID\":{\"id\":\"testID\",\"name\":\"testName\",\"disabledForwardingIds\":{},\"isCustomForwarding\":false,\"isCustomGreeting\":false,\"isCustomDirectConnect\":false,\"directConnect\":false,\"greetingId\":0,\"isCircle\":true,\"isCustomTranscriptionLanguage\":false,\"transcriptionLanguage\":\"\"},\"groupList\"";
		
		testGrouplist = Group.createGroupSettingsFromJsonResponse(testString);
		
		Assert.assertEquals(resultList.toString(), testGrouplist.toString());
	}
	
	@Test
	public void testListToJson() {
		
		testGrouplist.add(testGroup1);
		testGrouplist.add(testGroup3);
		
		resultString = "\"groups\":{\"testID\":{\"id\":\"testID\",\"greetingId\":0,\"isCustomForwarding\":false,\"isCustomGreeting\":false,\"disabledForwardingIds\":{},\"name\":\"testName\",\"isCustomDirectConnect\":false,\"directConnect\":false},\"testID1\":{\"id\":\"testID1\",\"greetingId\":0,\"isCustomForwarding\":false,\"isCustomGreeting\":false,\"disabledForwardingIds\":{\"3\":true,\"2\":true},\"name\":\"testName1\",\"isCustomDirectConnect\":false,\"directConnect\":false}}";
		
		Assert.assertEquals(resultString, Group.listToJson(testGrouplist).toString());
		
	}
	
	@Test
	public void testCreateArrayFromJsonObject() throws JSONException {
		
		testGroupObject.put(TEST_ID,testJSONOb1);
		
		final Group[] resultGroupArray = {testGroup};
		final Group[] testGroupArray = Group.createArrayFromJsonObject(testGroupObject);
		
		Assert.assertEquals(resultGroupArray[0].toString(), testGroupArray[0].toString());
		
	}
	
	@Test
	public void testCreateJSONObjectArrayFromJsonObject() throws JSONException {
		
		resultString = "{\"testID\":\"{id=testID;name=testName;isCustomDirectConnect=false;directConnect=false;isCustomGreeting=false;isCustomForwarding=false;greetingId=0;disabledForwardingIds=[]}\"}";
		
		testGroupObject.put("obj1", testJSONOb1);
		
		final JSONObject[] testObjectArray = Group.createJSONObjectArrayFromJsonObject(testGroupObject);
		
		Assert.assertEquals(resultString, testObjectArray[0].toString());
	}
	
	@Test
	public void testCreateJSONObjectFromJsonObject() throws JSONException {
		
		resultString = "{\"testID\":\"{id=testID;name=testName;isCustomDirectConnect=false;directConnect=false;isCustomGreeting=false;isCustomForwarding=false;greetingId=0;disabledForwardingIds=[]}\"}";
		
		testGroupObject.put("obj1", testJSONOb1);
		
		final JSONObject testObject = Group.createJSONObjectFromJsonObject(testGroupObject);
		
		Assert.assertEquals(resultString, testObject.toString());
		
	}
	
	@Test
	public void testGroupsArrayToJsonObject() throws JSONException {
		
		resultString = "{\"testID\":{\"id\":\"testID\",\"greetingId\":0,\"isCustomForwarding\":false,\"isCustomGreeting\":false,\"disabledForwardingIds\":{},\"name\":\"testName\",\"isCustomDirectConnect\":false,\"directConnect\":false}}";
		
		final Group[] testObjectArray = {testGroup};
		
		Assert.assertEquals(resultString, Group.groupsArrayToJsonObject(testObjectArray).toString());
	}
	
	@Test
	public void testIsPhoneDisabledFalseNullList() throws JSONException {
		


		final JSONObject testJSONOb4 = new JSONObject();
		testJSONOb4.put("id", TEST_ID);
		testJSONOb4.put("name", "testName");
		testJSONOb4.put("isCustomForwarding", false);
		testJSONOb4.put("isCustomGreeting", false);
		testJSONOb4.put("isCustomDirectConnect", false);
		testJSONOb4.put("directConnect", false);
		testJSONOb4.put("greetingId", 0);
		
		final Group testGroup4 = new Group(testJSONOb4);
		
		final boolean testNullList = testGroup4.isPhoneDisabled(1);
		
		Assert.assertEquals(false, testNullList);
		
		
	}

	@Test
	public void testIsPhoneDisabledFalseEmptyList() {

		final boolean test = testGroup.isPhoneDisabled(1);
		final boolean test1 = testGroup1.isPhoneDisabled(1);

		Assert.assertEquals(false, test);
		Assert.assertEquals(false, test1);
	}

	@Test
	public void testIsPhoneDisabledFalseNotInList() throws JSONException {

		final boolean test = testGroup2.isPhoneDisabled(1);
		final boolean test1 = testGroup3.isPhoneDisabled(1);

		Assert.assertEquals(false, test);
		Assert.assertEquals(false, test1);
	}

	@Test
	public void testIsPhoneDisabledTrue() throws JSONException {

		final boolean test = testGroup2.isPhoneDisabled(2);
		final boolean test1 = testGroup3.isPhoneDisabled(2);

		Assert.assertEquals(true, test);
		Assert.assertEquals(true, test1);
	}

}
