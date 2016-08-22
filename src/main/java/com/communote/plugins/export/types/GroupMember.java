package com.communote.plugins.export.types;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Pojo holding details of a group member.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class GroupMember {

    private long groupId;
    private long entityId;
    private boolean group;

    @XmlAttribute(required = true)
    public long getEntityId() {
        return entityId;
    }

    @XmlAttribute(required = true)
    public long getGroupId() {
        return groupId;
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

    public void setGroupId(long id) {
        this.groupId = id;
    }
}
