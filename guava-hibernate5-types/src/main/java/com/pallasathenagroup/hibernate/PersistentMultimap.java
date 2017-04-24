package com.pallasathenagroup.hibernate;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.hibernate.HibernateException;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Jan-Willem Gmelig Meyling
 */
public class PersistentMultimap extends AbstractPersistentCollection implements Multimap {

    protected Multimap map;

    public PersistentMultimap(SharedSessionContractImplementor session) {
        super(session);
    }

    public PersistentMultimap(SharedSessionContractImplementor session, Multimap map) {
        super(session);
        this.map = map;
        setInitialized();
    }

    public boolean empty() {
        return map.isEmpty();
    }

    public Collection getOrphans(Serializable snapshot, String entityName) {
        final Multimap sn = (Multimap) snapshot;
        return getOrphans( sn.values(), map.values(), entityName, getSession() );
    }

    public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner) {
        final Serializable[] array = (Serializable[]) disassembled;
        final int size = array.length; // 6
        beforeInitialize( persister, size );
        for ( int i = 0; i < size; i+=2 ) {
            map.put(
                persister.getIndexType().assemble( array[i] /* Tom */, getSession(), owner ),
                persister.getElementType().assemble( array[i+1] /* A */, getSession(), owner )
            );
        }
    }

    public Serializable disassemble(CollectionPersister persister) {
        final Serializable[] result = new Serializable[ map.size() * 2 ];
        final Iterator itr = map.entries().iterator();
        int i=0;
        while ( itr.hasNext() ) {
            final Map.Entry e = (Map.Entry) itr.next();
            result[i++] = persister.getIndexType().disassemble( e.getKey(), getSession(), null );
            result[i++] = persister.getElementType().disassemble( e.getValue(), getSession(), null );
        }
        return result;
    }


    public Iterator entries(CollectionPersister persister) {
        return map.entries().iterator();
    }

    private transient List<Object[]> loadingEntries;

    public Object readFrom(ResultSet rs, CollectionPersister role, CollectionAliases descriptor, Object owner) throws HibernateException, SQLException {
        final Object element = role.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() );
        if ( element != null ) {
            final Object index = role.readIndex( rs, descriptor.getSuffixedIndexAliases(), getSession() );
            if ( loadingEntries == null ) {
                loadingEntries = new ArrayList<>();
            }
            loadingEntries.add( new Object[] { index, element } );
        }
        return element;
    }

    @Override
    public boolean endRead() {
        if ( loadingEntries != null ) {
            for ( Object[] entry : loadingEntries ) {
                map.put( entry[0], entry[1] );
            }
            loadingEntries = null;
        }
        return super.endRead();
    }

    public Object getIndex(Object entry, int i, CollectionPersister persister) {
        return ((Map.Entry) entry).getKey();
    }

    public Object getElement(Object entry) {
        return ((Map.Entry) entry).getValue();
    }

    public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
        this.map = (Multimap) persister.getCollectionType().instantiate( anticipatedSize );
    }

    public Collection getSnapshotElement(Object entry, int i) {
        final Multimap sn = (Multimap) getSnapshot();
        // TODO VERIFY
        return sn.get( ( (Map.Entry) entry ).getKey() );
    }

    public boolean equalsSnapshot(CollectionPersister persister) {
        final Type elementType = persister.getElementType();
        final Multimap snapshotMap = (Multimap) getSnapshot();
        if ( snapshotMap.size() != this.map.size() ) {
            return false;
        }

        for (Object o : map.entries()) {
            Entry entry = (Entry) o;
//            if (! snapshotMap.containsEntry(entry.getKey(), entry.getValue())) {
//                return false;
//            }

            // TODO FIX:
            if (elementType.isDirty(entry.getValue(), snapshotMap.get( entry.getKey() ), getSession() ) ) {
                return false;
            }
        }
        return true;
    }

    public boolean isSnapshotEmpty(Serializable snapshot) {
        final Multimap snapshotMap = (Multimap) getSnapshot();
        return snapshotMap.isEmpty();
    }

    public Serializable getSnapshot(CollectionPersister persister) {
        final HashMultimap clonedMap = HashMultimap.create();

        for (Object o : map.entries()) {
            final Entry e = (Entry) o;
            final Object copy = persister.getElementType().deepCopy( e.getValue(), persister.getFactory() );
            clonedMap.put( e.getKey(), copy );
        }

        return clonedMap;
    }

    public boolean entryExists(Object entry, int i) {
        // TODO
        return ( (Map.Entry) entry ).getValue() != null;
    }

    public boolean needsInserting(Object entry, int i, Type elemType) {
        final Multimap sn = (Multimap) getSnapshot();
        final Map.Entry e = (Map.Entry) entry;
        // TODO
        return e.getValue() != null && sn.get( e.getKey() ) == null;
    }

    public boolean needsUpdating(Object entry, int i, Type elemType) {
        final Multimap sn = (Multimap) getSnapshot();
        final Map.Entry e = (Map.Entry) entry;
        final Object snValue = sn.get( e.getKey() );
        // TODO
        return e.getValue() != null
            && snValue != null
            && elemType.isDirty( snValue, e.getValue(), getSession() );
    }

    public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) {
        final List deletes = new ArrayList();
        for ( Object o : ((Map) getSnapshot()).entrySet() ) {
            final Entry e = (Entry) o;
            // TODO
            final Object key = e.getKey();
            if ( e.getValue() != null && map.get( key ) == null ) {
                deletes.add( indexIsFormula ? e.getValue() : key );
            }
        }
        return deletes.iterator();
    }

    @Override
    public boolean isWrapper(Object collection) {
        return map==collection;
    }

    @Override
    public int size() {
        return readSize() ? getCachedSize() : map.size();
    }

    @Override
    public boolean isEmpty() {
        return readSize() ? getCachedSize()==0 : map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        final Boolean exists = readIndexExistence( key );
        return exists == null ? map.containsKey( key ) : exists;    }

    @Override
    public boolean containsValue(Object value) {
        final Boolean exists = readElementExistence( value );
        return exists == null
            ? map.containsValue( value )
            : exists;
    }

    @Override
    public boolean containsEntry(Object key, Object value) {
        initialize(false);
        return map.containsEntry(key, value);
    }

    @Override
    public boolean put(Object key, Object value) {
        initialize(true);
        if (map.put(key, value)) {
            dirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object key, Object value) {
        initialize(false);
        if (map.remove(key, value)) {
            dirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean putAll(Object key, Iterable values) {
        initialize(false);
        if (map.putAll(key, values)) {
            dirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean putAll(Multimap multimap) {
        initialize(false);
        if (map.putAll(multimap)) {
            dirty();
            return true;
        }
        return false;
    }

    @Override
    public Collection replaceValues(Object key, Iterable values) {
        initialize(true);
        Collection replacedElements = map.replaceValues(key, values);
        if (! replacedElements.isEmpty()) {
            dirty();
        }
        return replacedElements;
    }

    @Override
    public Collection removeAll(Object key) {
        initialize(true);
        Collection removedElements = map.removeAll(key);
        if (! removedElements.isEmpty()) {
            dirty();
        }
        return removedElements;
    }

    @Override
    public void clear() {
        initialize(true);

        if (!map.isEmpty()) {
            dirty();
        }

        map.clear();
    }

    @Override
    public Collection get(Object key) {
        // TODO Improve
        read();
        return map.get(key);
    }

    @Override
    public Set keySet() {
        // TODO Improve
        read();
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    public Multiset keys() {
        // TODO Improve
        read();
        return ImmutableMultiset.copyOf(map.keys());
    }

    @Override
    public Collection values() {
        // TODO Improve
        initialize(true);
        return Collections.unmodifiableCollection(map.values());
    }

    @Override
    public Collection<Entry> entries() {
        // TODO Improve
        read();
        return Collections.unmodifiableCollection(map.entries());
    }

    @Override
    public Map asMap() {
        // TODO Improve
        read();
        return Collections.unmodifiableMap(map.asMap());
    }

    @Override
    public boolean equals(Object other) {
        read();
        return map.equals(other);
    }

    @Override
    public int hashCode() {
        read();
        return map.hashCode();
    }

}
