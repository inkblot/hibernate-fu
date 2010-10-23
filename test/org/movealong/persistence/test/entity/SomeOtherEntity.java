package org.movealong.persistence.test.entity;

import javax.persistence.*;

/**
 * Created by IntelliJ IDEA.
 * User: inkblot
 * Date: Oct 21, 2010
 * Time: 7:07:32 PM
 */
@Entity
@Table(name = "some_others")
public class SomeOtherEntity {
    private Long id;
    private String name;

    @Id
    @Column(name = "other_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "thing_name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
