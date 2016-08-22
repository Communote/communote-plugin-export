package com.communote.plugins.export.exporter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.communote.plugins.export.serializer.SerializerWriteException;
import com.communote.plugins.export.serializer.StreamingSerializer;
import com.communote.plugins.export.types.Topic;
import com.communote.plugins.export.types.TopicMember;
import com.communote.server.api.ServiceLocator;
import com.communote.server.model.blog.Blog;
import com.communote.server.model.blog.BlogConstants;
import com.communote.server.model.blog.BlogMember;
import com.communote.server.model.blog.BlogRole;
import com.communote.server.model.user.CommunoteEntity;
import com.communote.server.model.user.User;
import com.communote.server.persistence.helper.dao.LazyClassLoaderHelper;

/**
 * Exporter for topics with members and their roles.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class TopicExporter implements Exporter<Topic> {

    private Topic convert(Blog source, TagToStringConverter tagsConverter) {
        Topic target = new Topic();
        target.setAlias(source.getNameIdentifier());
        target.setCreationDate(new Date(source.getCreationDate().getTime()));
        target.setDescription(source.getDescription());
        target.setId(source.getId());
        target.setTitle(source.getTitle());
        if (source.getTags() != null) {
            target.setTags(tagsConverter.convert(source.getTags()));
        }
        if (source.isAllCanWrite()) {
            target.setAllUsersRole(TopicMember.Role.WRITE);
        } else if (source.isAllCanRead()) {
            target.setAllUsersRole(TopicMember.Role.READ);
        } else {
            target.setAllUsersRole(TopicMember.Role.NONE);
        }
        if (source.getMembers() != null) {
            List<TopicMember> targetMembers = new ArrayList<>();
            target.setMembers(targetMembers);
            for (BlogMember member : source.getMembers()) {
                CommunoteEntity entity = LazyClassLoaderHelper.deproxy(member.getMemberEntity(),
                        CommunoteEntity.class);
                boolean add = false;
                boolean isGroup = false;
                if (entity instanceof User) {
                    User user = (User) entity;
                    // only export members that are active, disabled or deleted
                    add = Utils.isUserActiveOrDisabled(user);
                } else {
                    isGroup = true;
                    add = true;
                }
                if (add) {
                    TopicMember targetMember = new TopicMember();
                    targetMember.setEntityId(member.getMemberEntity().getId());
                    targetMember.setGroup(isGroup);
                    targetMember.setRole(convertRole(member.getRole()));
                    targetMembers.add(targetMember);
                }
            }
        }
        return target;
    }

    private TopicMember.Role convertRole(BlogRole source) {
        if (BlogRole.MANAGER.equals(source)) {
            return TopicMember.Role.MANAGE;
        } else if (BlogRole.MEMBER.equals(source)) {
            return TopicMember.Role.WRITE;
        } else {
            return TopicMember.Role.READ;
        }
    }

    @Override
    public void export(StreamingSerializer<Topic> serializer) throws ExportFailedException {
        try {
            serializer.prepareSerialization();
            exportTopics(serializer);
            serializer.finishSerialization();
        } catch (SerializerWriteException e) {
            throw new ExportFailedException("Exporting the topics failed", e);
        }
    }

    private void exportTopics(StreamingSerializer<Topic> serializer)
            throws SerializerWriteException, ExportFailedException {
        SessionFactory sessionFactory = ServiceLocator.findService(SessionFactory.class);
        Session session = sessionFactory.openSession();
        TagToStringConverter tagsConverter = new TagToStringConverter();
        try {
            try {
                Query query = session.createQuery("from " + BlogConstants.CLASS_NAME);
                query.setCacheMode(CacheMode.GET);
                query.setReadOnly(true);
                // TODO MySQL will load the result set completely into memory, this can be tuned
                // with query.setFetchSize(Integer.MIN_VALUE) which leads to loading the results row
                // by row. But setting this fetch-size causes an exception with PostgreSQL.
                ScrollableResults topics = query.scroll(ScrollMode.FORWARD_ONLY);
                long count = 0l;
                while (topics.next()) {
                    Blog topic = (Blog) topics.get(0);
                    serializer.appendEntity(convert(topic, tagsConverter));
                    if (++count % 20 == 0) {
                        session.flush();
                        session.clear();
                    }
                }
                // TODO necessary? Guess it is, because we do not have a transaction which when
                // closed would take care of closing the results
                topics.close();
            } finally {
                session.close();
            }
        } catch (HibernateException e) {
            throw new ExportFailedException("Exporting the topics failed", e);
        }

    }

}
