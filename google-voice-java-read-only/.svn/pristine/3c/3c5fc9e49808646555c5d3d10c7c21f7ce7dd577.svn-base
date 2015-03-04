package test.util;

import java.util.ArrayList;
import java.util.List;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import org.junit.Assert;
import org.junit.Test;

import com.techventus.server.voice.util.ParsingUtil;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 *
 */
// Coverage Holes:
//	removeUninterestingParts line 41 unable to make null.
//	removeUninterestingParts line 48 unable to make null.
public class ParsingUtilTest { 
	
	private final static String TEST_TEXT = "Hello Toby, How are you today? Fine.";
	private final static String TEST_START_BORDER = "How are";
	private final static String TEST_END_BORDER = "?";
	private final static String ARRAY_ONE = "one";
	private final static String TEST_KEY = "testKey";
	
	JSONArray testJSONArray = new JSONArray(); 
	JSONObject testJSONObject = new JSONObject(); 
	
	@Test
	public void testRemoveUninterestingPartsNullEndBorder() {

		Assert.assertEquals(null, ParsingUtil.removeUninterestingParts(
				TEST_TEXT, TEST_START_BORDER, null, true));
	}

	@Test
	public void testRemoveUninterestingPartsNullStartBorder() {

		Assert.assertEquals(null, ParsingUtil.removeUninterestingParts(
				TEST_TEXT, null, TEST_END_BORDER, true));
	}

	@Test
	public void testRemoveUninterestingPartsNullText() {

		Assert.assertEquals(null, ParsingUtil.removeUninterestingParts(null,
				TEST_START_BORDER, TEST_END_BORDER, true));
	}

	@Test
	public void testRemoveUninterestingPartsIncludeBordersNoSuchStartIndex() {

		Assert.assertNull(ParsingUtil.removeUninterestingParts(
				TEST_TEXT, "Does Not Exist", TEST_END_BORDER,
				true));
	}

	@Test
	public void testRemoveUninterestingPartsIncludeBordersNoSuchEndIndex() {

		Assert.assertNull(ParsingUtil.removeUninterestingParts(
				TEST_TEXT, TEST_START_BORDER,
				"Does not exist", true));
	}

	@Test
	public void testRemoveUninterestingPartsIncludeBorders() {

		Assert.assertEquals("How are you today?", ParsingUtil
				.removeUninterestingParts(
						TEST_TEXT, TEST_START_BORDER, TEST_END_BORDER,
						true));
	}

	@Test
	public void testRemoveUninterestingPartsIncludeBordersFalse() {

		Assert.assertEquals(" you today", ParsingUtil.removeUninterestingParts(
				TEST_TEXT, TEST_START_BORDER, TEST_END_BORDER, false));
	}

	@Test
	public void testJsonIntArrayToIntArrayPass() {
		
		testJSONArray.put(1);
		testJSONArray.put(2);

		final int[] testArray = { 1, 2 };

		Assert.assertArrayEquals(testArray,
				ParsingUtil.jsonIntArrayToIntArray(testJSONArray));

	}

	@Test
	public void testJsonStringArrayToStringArrayPass() {

		testJSONArray.put(ARRAY_ONE);

		final String[] testArray = { ARRAY_ONE };

		final String[] testJSString = ParsingUtil
				.jsonStringArrayToStringArray(testJSONArray);

		Assert.assertEquals(testArray[0], testJSString[0]);
	}

	@Test
	public void testJsonStringArrayToStringList() throws JSONException {

		testJSONArray.put(ARRAY_ONE);
		testJSONArray.put("two");

		testJSONObject.put(TEST_KEY, testJSONArray);

		final List<String> testInputList = new ArrayList<String>();
		final List<String> testList = new ArrayList<String>();
		testList.add(ARRAY_ONE);
		testList.add("two");

		Assert.assertEquals(
				testList.toString(),
				ParsingUtil.jsonStringArrayToStringList(testJSONObject,
						testInputList, TEST_KEY).toString());

	}
	
	@Test
	public void testJsonIntArrayToIntegerList() throws JSONException {
		
		testJSONArray.put(1);
		testJSONArray.put(2);

		testJSONObject.put(TEST_KEY, testJSONArray);

		final List<Integer> testInputList = new ArrayList<Integer>();
		final List<Integer> testList = new ArrayList<Integer>();
		testList.add(1);
		testList.add(2);

		Assert.assertEquals(
				testList.toString(),
				ParsingUtil.jsonIntArrayToIntegerList(testJSONObject,
						testInputList, TEST_KEY).toString());
		
	}

}
