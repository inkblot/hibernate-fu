package org.movealong.persistence.test.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by IntelliJ IDEA.
 * User: inkblot
 * Date: Oct 21, 2010
 * Time: 7:06:15 PM
 */
@Entity
@Table(name = "things")
public class SomeEntity {
    private Long id;

    @Id
    @Column(name = "thing_id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
