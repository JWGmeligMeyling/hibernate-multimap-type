package com.pallasathenagroup.hibernate;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Jan-Willem Gmelig Meyling
 */
public class MultimapType implements UserCollectionType {

    public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister) throws HibernateException {
        return new PersistentMultimap(session);
    }

    public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
        return new PersistentMultimap(session, (Multimap) collection);
    }

    public Iterator getElementsIterator(Object collection) {
        return ((Multimap) collection).values().iterator();
    }

    public boolean contains(Object collection, Object entity) {
        throw new UnsupportedOperationException();
    }

    public Object indexOf(Object collection, Object entity) {
        throw new UnsupportedOperationException();
    }

    public Object replaceElements(Object original, Object target, CollectionPersister persister, Object owner, Map copyCache, SharedSessionContractImplementor session) throws HibernateException {
        return replaceElements((Multimap) original, (Multimap) target, persister, owner, copyCache, session);
    }

    private  <K,V> Multimap<K,V> replaceElements(Multimap<K,V> original, Multimap<K,V> target, CollectionPersister persister, Object owner, Map copyCache, SharedSessionContractImplementor session) throws HibernateException {
        target.clear();

        for (Entry<K, V> entry : original.entries()) {
            K key = (K) persister.getIndexType().replace(entry.getKey(), null, session, owner, copyCache );
            V value = (V) persister.getElementType().replace(entry.getValue(), null, session, owner, copyCache );
            target.put(key, value);
        }

        return target;
    }

    public Object instantiate(int anticipatedSize) {
        return anticipatedSize <= 0 ?
            HashMultimap.create() :
            HashMultimap.create(anticipatedSize, anticipatedSize);
    }
}
