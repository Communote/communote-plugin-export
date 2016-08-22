package com.communote.plugins.export.types;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;

/**
 * Pojo holding the details of a member of a topic.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class TopicMember {

    @XmlEnum
    public enum Role {
        READ,
        WRITE,
        MANAGE,
        NONE;
    }

    private long entityId;
    private boolean group;
    private Role role;

    @XmlAttribute(required = true)
    public long getEntityId() {
        return entityId;
    }

    @XmlAttribute(required = true)
    public Role getRole() {
        return role;
    }

    @XmlAttribute
    public boolean isGroup() {
        return group;
    }

    public void setEntityId(long id) {
        this.entityId = id;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
