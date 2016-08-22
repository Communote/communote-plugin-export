package com.communote.plugins.export.types;

import javax.xml.bind.annotation.XmlElement;

/**
 * Pojo holding the details of a group.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class Group extends IdentifiableEntity {
    private String groupName;
    private String description;
    private String ldapExternalId;

    @XmlElement
    public String getDescription() {
        return description;
    }

    @XmlElement(required = true)
    public String getGroupName() {
        return groupName;
    }

    @XmlElement
    public String getLdapExternalId() {
        return ldapExternalId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setLdapExternalId(String ldapExternalId) {
        this.ldapExternalId = ldapExternalId;
    }

}
