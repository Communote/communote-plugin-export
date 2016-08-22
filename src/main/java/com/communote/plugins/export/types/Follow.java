package com.communote.plugins.export.types;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * Pojo holding the followed entities of a user.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class Follow {

    private long userId;
    private Collection<IdentifiableEntity> followedTopics;
    private Collection<IdentifiableEntity> followedUsers;
    private Collection<String> followedTags;

    @XmlElementWrapper
    @XmlElement(name = "Tag")
    public Collection<String> getFollowedTags() {
        return followedTags;
    }

    @XmlElementWrapper
    @XmlElement(name = "Topic")
    public Collection<IdentifiableEntity> getFollowedTopics() {
        return followedTopics;
    }

    @XmlElementWrapper
    @XmlElement(name = "User")
    public Collection<IdentifiableEntity> getFollowedUsers() {
        return followedUsers;
    }

    @XmlAttribute(required = true)
    public long getUserId() {
        return userId;
    }

    public void setFollowedTags(Collection<String> followedTags) {
        this.followedTags = followedTags;
    }

    public void setFollowedTopics(Collection<IdentifiableEntity> followedTopics) {
        this.followedTopics = followedTopics;
    }

    public void setFollowedUsers(Collection<IdentifiableEntity> followedUsers) {
        this.followedUsers = followedUsers;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}
