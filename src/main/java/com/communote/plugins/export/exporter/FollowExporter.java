package com.communote.plugins.export.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.communote.plugins.export.serializer.SerializerWriteException;
import com.communote.plugins.export.serializer.StreamingSerializer;
import com.communote.plugins.export.types.Follow;
import com.communote.plugins.export.types.IdentifiableEntity;
import com.communote.server.api.ServiceLocator;
import com.communote.server.model.tag.TagImpl;
import com.communote.server.persistence.user.UserDao;

/**
 * Exporter which extracts which topics, tags and users are followed by the Communote users.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class FollowExporter implements Exporter<Follow> {

    private final List<Long> usersToExport;

    /**
     * Create a new exporter for the followed items of the provided users
     *
     * @param usersToExport
     *            the users whose follows should be exported. This should be the users that were
     *            exported by the UserExporter.
     */
    public FollowExporter(List<Long> usersToExport) {
        this.usersToExport = usersToExport;
    }

    private Collection<IdentifiableEntity> convertIds(List<Long> ids, Collection<Long> filter) {
        if (ids == null || ids.size() == 0) {
            return null;
        }
        ArrayList<IdentifiableEntity> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            if (filter == null || filter.contains(id)) {
                result.add(new IdentifiableEntity(id));
            }
        }
        return result;
    }

    private List<String> convertTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.size() == 0) {
            return null;
        }
        Session session = ServiceLocator.findService(SessionFactory.class).openSession();
        session.setFlushMode(FlushMode.MANUAL);
        session.setCacheMode(CacheMode.GET);
        ArrayList<String> result = new ArrayList<String>(tagIds.size());
        try {
            for (Long tagId : tagIds) {
                result.add(((TagImpl) session.get(TagImpl.class, tagId)).getDefaultName());
            }
        } finally {
            session.close();
        }
        return result;
    }

    @Override
    public void export(StreamingSerializer<Follow> serializer) throws ExportFailedException {
        try {
            serializer.prepareSerialization();
            UserDao userDao = ServiceLocator.findService(UserDao.class);
            for (Long userId : usersToExport) {
                Follow follow = exportFollowsOfUser(userId, userDao);
                if (follow != null) {
                    serializer.appendEntity(follow);
                }
            }
            serializer.finishSerialization();
        } catch (SerializerWriteException e) {
            throw new ExportFailedException("Exporting the follows failed", e);
        }

    }

    private Follow exportFollowsOfUser(Long userId, UserDao userDao) {
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowedTopics(convertIds(userDao.getFollowedBlogs(userId, -1l, -1l), null));
        // only consider followed users that were passed to the exporter, because we assume that
        // those are the active, temporarily and permanently disabled users
        follow.setFollowedUsers(convertIds(userDao.getFollowedUsers(userId, -1l, -1l),
                this.usersToExport));
        follow.setFollowedTags(convertTagIds(userDao.getFollowedTags(userId, -1l, -1l)));
        if (follow.getFollowedTopics() != null || follow.getFollowedUsers() != null
                || follow.getFollowedTags() != null) {
            return follow;
        }
        return null;
    }

}
