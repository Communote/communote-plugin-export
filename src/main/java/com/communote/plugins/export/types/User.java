package com.communote.plugins.export.types;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;

/**
 * Pojo holding the details of a user.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class User extends IdentifiableEntity {

    @XmlEnum
    public enum Status {
        ACTIVE,
        DELETED,
        DISABLED;
    }

    private String alias;
    private String email;
    private String firstname;
    private String lastname;
    private String language;
    private Status status;
    private String ldapDN;
    private String ldapExternalId;
    private Collection<String> tags;

    @XmlElement(required = true)
    public String getAlias() {
        return alias;
    }

    @XmlElement(required = true)
    public String getEmail() {
        return email;
    }

    @XmlElement
    public String getFirstname() {
        return firstname;
    }

    @XmlElement
    public String getLanguage() {
        return language;
    }

    @XmlElement
    public String getLastname() {
        return lastname;
    }

    @XmlElement
    public String getLdapDN() {
        return ldapDN;
    }

    @XmlElement
    public String getLdapExternalId() {
        return ldapExternalId;
    }

    @XmlElement(required = true)
    public Status getStatus() {
        return status;
    }

    @XmlElementWrapper
    @XmlElement(name = "Tag")
    public Collection<String> getTags() {
        return tags;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void setLdapDN(String ldapDN) {
        this.ldapDN = ldapDN;
    }

    public void setLdapExternalId(String ldapExternalId) {
        this.ldapExternalId = ldapExternalId;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setTags(Collection<String> tags) {
        this.tags = tags;
    }

}
