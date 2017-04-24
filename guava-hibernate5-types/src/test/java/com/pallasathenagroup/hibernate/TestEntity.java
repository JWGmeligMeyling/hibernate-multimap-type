package com.pallasathenagroup.hibernate;

import com.google.common.collect.Multimap;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;

/**
 * @author Jan-Willem Gmelig Meyling
 */
@Entity
@TypeDef(name = "MultimapType", typeClass = MultimapType.class)
public class TestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection(targetClass = Integer.class)
    @CollectionType(type = "MultimapType")
    @MapKeyColumn(name="key")
    @Column(name="value")
//    @CollectionTable(name="example_attributes", joinColumns=@JoinColumn(name="id"))
    private Multimap<Integer, Integer> simpleMultiMap;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Multimap<Integer, Integer> getSimpleMultiMap() {
        return simpleMultiMap;
    }

    public void setSimpleMultiMap(Multimap<Integer, Integer> simpleMultiMap) {
        this.simpleMultiMap = simpleMultiMap;
    }
}
