package endpoint.repository.query;

import static endpoint.repository.query.Condition.and;
import static endpoint.repository.query.Condition.c;
import static endpoint.repository.query.Condition.or;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import endpoint.repository.SimpleObject;
import endpoint.repository.annotations.Id;
import endpoint.repository.models.basic.BasicObject;
import endpoint.utils.DateUtils;
import endpoint.utils.EndpointTestCase;

public class NewDatastoreQueryTest extends EndpointTestCase {

	private void saveThreeObjects() {
		r.save(new SimpleObject(1, 1l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object1"));
		r.save(new SimpleObject(1, 1l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object2"));
		r.save(new SimpleObject(1, 1l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object3"));
	}

	private void saveManyBasicObjects(int n) {
		for (int i = 0; i < n; i++) {
			BasicObject object = new BasicObject();
			object.setStringValue("xpto");
			object.setIntValue(i);
			r.save(object);
		}
	}

	@Test
	public void testWhere() {
		saveManyBasicObjects(3);
		r.save(new BasicObject("different"));

		List<BasicObject> objects = r.query(BasicObject.class).where("stringValue", "=", "xpto").list();

		assertEquals(3, objects.size());

		assertEquals(0, objects.get(0).getIntValue());
		assertEquals(1, objects.get(1).getIntValue());
		assertEquals(2, objects.get(2).getIntValue());
	}

	@Test
	public void testWhereWithUnicode() {
		r.save(new BasicObject("\u00c1"));

		List<BasicObject> objects = r.query(BasicObject.class).where("stringValue", "=", "\u00c1").list();

		assertEquals(1, objects.size());
		assertEquals("\u00c1", objects.get(0).getStringValue());
	}

	@Test
	public void testChainedWheres() {
		saveManyBasicObjects(1);

		List<BasicObject> objects = r.query(BasicObject.class).where("intValue", "=", 0).where("stringValue", "=", "xpto").list();

		assertEquals(1, objects.size());
		assertEquals("xpto", objects.get(0).getStringValue());
		assertEquals(0, objects.get(0).getIntValue());
	}

	@Test
	public void testChainedWheresWithAnd() {
		saveManyBasicObjects(1);

		List<BasicObject> objects = r.query(BasicObject.class).where(and(c("intValue", "=", 0), c("stringValue", "=", "xpto"))).list();

		assertEquals(1, objects.size());
		assertEquals("xpto", objects.get(0).getStringValue());
		assertEquals(0, objects.get(0).getIntValue());
	}

	@Test
	public void testWhereWithOr() {
		saveManyBasicObjects(2);

		List<BasicObject> objects = r.query(BasicObject.class).where(or(c("intValue", "=", 0), c("intValue", "=", 1))).list();

		assertEquals(2, objects.size());
		Collections.sort(objects, new Comparator<BasicObject>() {
			@Override
			public int compare(BasicObject o1, BasicObject o2) {
				return o1.getIntValue() - o2.getIntValue();
			}
		});

		assertEquals(2, objects.size());

		assertEquals("xpto", objects.get(0).getStringValue());
		assertEquals(0, objects.get(0).getIntValue());

		assertEquals("xpto", objects.get(1).getStringValue());
		assertEquals(1, objects.get(1).getIntValue());
	}

	@Test
	public void testWhereWithComplexAndOrStructure() {
		saveThreeObjects();
		List<SimpleObject> objects = r
				.query(SimpleObject.class)
				.where(or(and(c("aString", "=", "object2"), c("aString", "=", "object1")),
						and(c("aString", "=", "object2"), c("aString", "=", "object2")))).list();

		assertEquals(1, objects.size());
		objects.get(0).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object2");

		objects = r
				.query(SimpleObject.class)
				.where(or(and(c("aString", "=", "object1"), c("aString", "=", "object1")),
						and(c("aString", "=", "object2"), c("aString", "=", "object1")))).list();

		assertEquals(1, objects.size());
		objects.get(0).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object1");
	}

	@Test
	public void testChainedWheresMultipleStatements() {
		saveThreeObjects();
		r.save(new SimpleObject(1, 2l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object2"));

		List<SimpleObject> objects = r.query(SimpleObject.class).where("aLong", "=", 1l).where("aString", "=", "object2").list();

		assertEquals(1, objects.size());
		objects.get(0).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object2");
	}

	@Test
	public void testOptions() {
		saveThreeObjects();
		r.save(new SimpleObject(1, 2l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object4"));

		DatastoreQueryOptions options = DatastoreQueryOptions
				.parse("{where: ['aLong', '=', 1], order: [{p:'aString', d:'desc'}], limit: 2}");

		List<SimpleObject> objects = r.query(SimpleObject.class).options(options).list();

		assertEquals(2, objects.size());
		objects.get(0).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object3");
		objects.get(1).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object2");
	}

	@Test
	public void testOrderWithUnicode() {
		saveThreeObjects();
		// 'A' with accent
		r.save(new SimpleObject(1, 1l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "\u00c1"));

		List<SimpleObject> objects = r.query(SimpleObject.class).order("aString", "desc").list();

		assertEquals(4, objects.size());
		objects.get(0).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object3");
		objects.get(1).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object2");
		objects.get(2).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object1");
		objects.get(3).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "\u00c1");
	}

	@Test
	public void testOrderWithTwoProperties() {
		saveThreeObjects();
		r.save(new SimpleObject(1, 2l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object2"));
		r.save(new SimpleObject(1, 3l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object3"));

		List<SimpleObject> objects = r.query(SimpleObject.class).order("aString", "desc").order("aLong", "desc").list();

		objects.get(0).assertObject(1, 3l, 1.1, true, "2013/12/26 23:55:01", "object3");
		objects.get(1).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object3");
		objects.get(2).assertObject(1, 2l, 1.1, true, "2013/12/26 23:55:01", "object2");
		objects.get(3).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object2");
		objects.get(4).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object1");
	}

	@Test
	public void testSort() {
		saveThreeObjects();
		r.save(new SimpleObject(1, 2l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object2"));
		r.save(new SimpleObject(1, 3l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object3"));

		List<SimpleObject> objects = r.query(SimpleObject.class).sort("aString", "desc").sort("aLong", "desc").list();

		objects.get(0).assertObject(1, 3l, 1.1, true, "2013/12/26 23:55:01", "object3");
		objects.get(1).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object3");
		objects.get(2).assertObject(1, 2l, 1.1, true, "2013/12/26 23:55:01", "object2");
		objects.get(3).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object2");
		objects.get(4).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object1");
	}

	@Test
	public void testLimit() {
		saveThreeObjects();

		List<SimpleObject> objects = r.query(SimpleObject.class).order("aString", "desc").limit(1).list();

		assertEquals(1, objects.size());
		objects.get(0).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object3");
	}

	@Test
	public void testCursor() {
		saveThreeObjects();

		DatastoreQuery<SimpleObject> q = r.query(SimpleObject.class).order("aString", "desc").limit(1);

		List<SimpleObject> objects = q.list();
		assertEquals(1, objects.size());
		objects.get(0).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object3");

		objects = q.list();
		assertEquals(1, objects.size());
		objects.get(0).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object2");

		objects = r.query(SimpleObject.class).cursor(q.getCursor()).order("aString", "desc").limit(1).list();
		assertEquals(1, objects.size());
		objects.get(0).assertObject(1, 1l, 1.1, true, "2013/12/26 23:55:01", "object1");
	}

	@Test
	public void testFindByIdUsingWhere() {
		SimpleObject object = new SimpleObject("xpto");

		r.save(object);

		object = r.query(SimpleObject.class).where("id", "=", object.getId()).first();
		assertEquals("xpto", object.getAString());
	}

	@Test
	public void testFindByIdUsingWhereWithLongId() {
		ObjectWithLongId object = new ObjectWithLongId("xpto");

		r.save(object);

		object = r.query(ObjectWithLongId.class).where("key", "=", object.key).first();
		assertEquals("xpto", object.text);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testFindById() {
		SimpleObject object = new SimpleObject("xpto");

		r.save(object);

		object = r.query(SimpleObject.class).id(object.getId());
		assertEquals("xpto", object.getAString());
	}

	@Test
	public void testIn() {
		SimpleObject object1 = new SimpleObject("xpto1");
		r.save(object1);

		SimpleObject object2 = new SimpleObject("xpto2");
		r.save(object2);

		List<SimpleObject> objects = r.query(SimpleObject.class).where("id", "in", Arrays.asList(object1.getId())).list();
		assertEquals(1, objects.size());
	}

	@Test
	public void testWhereInEmptyList() {
		saveThreeObjects();
		r.save(new SimpleObject(1, 2l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object4"));

		List<SimpleObject> objects = r.query(SimpleObject.class).where("aLong", "in", Collections.emptyList()).list();

		assertEquals(0, objects.size());
	}

	@Test
	public void testWhereInEmptyListWithinOrWithTrue() {
		saveThreeObjects();
		r.save(new SimpleObject(1, 2l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object4"));
		BaseCondition falseCondition = Condition.c("aLong", "in", Collections.emptyList());
		BaseCondition condition = Condition.or(falseCondition, c("aLong", "=", 1l));

		List<SimpleObject> objects = r.query(SimpleObject.class).where(condition).list();
		assertEquals(3, objects.size());
	}

	@Test
	public void testWhereInEmptyListWithinOrWithFalse() {
		saveThreeObjects();
		r.save(new SimpleObject(1, 2l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object4"));
		BaseCondition falseCondition = Condition.c("aLong", "in", Collections.emptyList());
		BaseCondition condition = Condition.or(falseCondition, falseCondition);

		List<SimpleObject> objects = r.query(SimpleObject.class).where(condition).list();
		assertEquals(0, objects.size());
	}

	@Test
	public void testWhereInEmptyListWithinAndWithTrue() {
		saveThreeObjects();
		r.save(new SimpleObject(1, 2l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object4"));
		BaseCondition falseCondition = Condition.c("aLong", "in", Collections.emptyList());
		BaseCondition condition = Condition.and(falseCondition, c("aLong", "=", 1l));

		List<SimpleObject> objects = r.query(SimpleObject.class).where(condition).list();
		assertEquals(0, objects.size());
	}

	@Test
	public void testWhereInEmptyListWithinAndWithFalse() {
		saveThreeObjects();
		r.save(new SimpleObject(1, 2l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object4"));
		BaseCondition falseCondition = Condition.c("aLong", "in", Collections.emptyList());
		BaseCondition condition = Condition.and(falseCondition, falseCondition);

		List<SimpleObject> objects = r.query(SimpleObject.class).where(condition).list();
		assertEquals(0, objects.size());
	}

	@Test
	public void testWhereInEmptyListWithinAndAndOr() {
		saveThreeObjects();
		r.save(new SimpleObject(1, 2l, 1.1, true, DateUtils.toTimestamp("2013/12/26 23:55:01"), "object4"));
		BaseCondition falseCondition = Condition.c("aLong", "in", Collections.emptyList());
		BaseCondition condition = Condition.or(falseCondition, Condition.and(c("aLong", "=", 1l), falseCondition));

		List<SimpleObject> objects = r.query(SimpleObject.class).where(condition).list();
		assertEquals(0, objects.size());
	}

	public static class ObjectWithLongId {

		@Id
		Long key;

		String text;

		public ObjectWithLongId() {
		}

		public ObjectWithLongId(String text) {
			this.text = text;
		}
	}

}