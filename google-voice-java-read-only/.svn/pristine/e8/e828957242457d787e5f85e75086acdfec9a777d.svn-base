package test.datatypes;

import java.util.ArrayList;
import java.util.List;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import org.junit.Assert;
import org.junit.Test;

import com.techventus.server.voice.datatypes.DisabledForwardingId;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 *
 */
public class DisabledForwardingIdTest {

	// Golden DisabledForwardingId Object
	final DisabledForwardingId goldDFId = new DisabledForwardingId(
			"1", true);

	// test Objects
	DisabledForwardingId testDFId;
	JSONObject testJsonDFId = new JSONObject();
	JSONArray testJSArray = new JSONArray();

	@Test
	public void testDisabledForwardingNullObjectSaveModeTrue()
			throws JSONException {

		testDFId = new DisabledForwardingId(
				testJsonDFId, true);

		Assert.assertEquals("{id=null;disabled=false}", testDFId.toString());
	}

	@Test
	public void testDisabledForwardingNullObjectSaveModefalse() {

		try {
			testDFId = new DisabledForwardingId(
					testJsonDFId, false);
		} catch (Exception e) {
			testDFId = goldDFId;
		}

		Assert.assertEquals(goldDFId, testDFId);

	}

	@Test
	public void testDisabledForwardingIdSaveModeFalse() throws JSONException {

		testJsonDFId.put("id", "1");
		testJsonDFId.put("disabled", true);

		testDFId = new DisabledForwardingId(
				testJsonDFId, false);

		final boolean test = goldDFId.toString().equals(
				testDFId.toString());

		Assert.assertEquals(true, test);
	}

	@Test
	public void testDisabledForwardingIdTrueSaveModeTrue()
			throws JSONException {

		testJsonDFId.put("id", "1");
		testJsonDFId.put("disabled", true);

		testDFId = new DisabledForwardingId(
				testJsonDFId, true);

		final boolean test = goldDFId.toString().equals(
				testDFId.toString());

		Assert.assertEquals(true, test);
	}

	@Test
	public void testCreateDisabledForwardingIdListFromJsonPartResponse()
			throws JSONException {

		final List<DisabledForwardingId> testList = new ArrayList<DisabledForwardingId>();
		testList.add(goldDFId);

		testJSArray.put(1);
		final JSONObject disabledFIDs = testJSArray.toJSONObject(testJSArray);

		Assert.assertEquals(
				testList.toString(),
				DisabledForwardingId
						.createDisabledForwardingIdListFromJsonPartResponse(
								disabledFIDs.toString()).toString());
	}

	@Test
	public void testCreateDisabledForwardingIdArrayFromJsonPartResponse()
			throws JSONException {

		final DisabledForwardingId[] goldArray = { goldDFId };

		testJSArray.put(1);
		final JSONObject disabledFIDs = testJSArray.toJSONObject(testJSArray);

		Assert.assertEquals(
				goldArray[0].toString(),
				DisabledForwardingId
						.createDisabledForwardingIdArrayFromJsonPartResponse(disabledFIDs
								.toString())[0].toString());

	}

	@Test
	public void testArrayToJsonObject() throws JSONException {

		final List<DisabledForwardingId> testList = new ArrayList<DisabledForwardingId>();
		testList.add(goldDFId);	

		testJsonDFId.put("1", true);
		
		Assert.assertEquals(testJsonDFId.toString(), DisabledForwardingId
				.arrayToJsonObject(testList).toString());

	}

}
