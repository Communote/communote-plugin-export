package com.communote.plugins.export.types;

import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Pojo holding details of a like of a note.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class Like {
    private Date creationDate;
    private long userId;

    public Like() {
    }

    public Like(long userId, Date creationDate) {
        this.userId = userId;
        this.creationDate = creationDate;
    }

    @XmlAttribute(required = true)
    public Date getCreationDate() {
        return creationDate;
    }

    @XmlAttribute(required = true)
    public long getUserId() {
        return userId;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}
