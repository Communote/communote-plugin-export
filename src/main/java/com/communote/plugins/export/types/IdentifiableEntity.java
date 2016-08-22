package com.communote.plugins.export.types;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Pojo for any entity which can be identified by a numeric ID.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class IdentifiableEntity {
    private long id;

    public IdentifiableEntity() {
    }

    public IdentifiableEntity(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        IdentifiableEntity other = (IdentifiableEntity) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }

    @XmlAttribute(required = true)
    public long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    public void setId(long id) {
        this.id = id;
    }

}
