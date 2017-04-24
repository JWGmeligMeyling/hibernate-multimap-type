package com.pallasathenagroup.hibernate;

import com.google.common.collect.HashMultimap;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author Jan-Willem Gmelig Meyling
 */
public class TestEntityTest extends BaseNonConfigCoreFunctionalTestCase {

    @Test
    public void testPersist() throws Exception {
        HashMultimap<Integer, Integer> map = HashMultimap.create();
        map.put(1, 1);

        TestEntity testEntity = new TestEntity();
        testEntity.setSimpleMultiMap(map);

        openSession().persist(testEntity);
    }

    @Test
    public void testPersistAndGet() throws Exception {
        HashMultimap<Integer, Integer> map = HashMultimap.create();
        map.put(1, 1);

        openSession();

        TestEntity testEntity = new TestEntity();
        testEntity.setSimpleMultiMap(map);

        getSession().getTransaction().begin();
        getSession().persist(testEntity);
        getSession().getTransaction().commit();
        getSession().clear();

        TestEntity actualEntity = getSession().find(TestEntity.class, 1l);
        assertNotSame(actualEntity, testEntity);
        assertNotSame(actualEntity.getSimpleMultiMap(), testEntity.getSimpleMultiMap());
        assertEquals(actualEntity.getSimpleMultiMap(), testEntity.getSimpleMultiMap());


    }

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class<?>[]{ TestEntity.class};
    }

}
