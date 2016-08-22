package com.communote.plugins.export.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.communote.common.util.PageableList;
import com.communote.plugins.export.serializer.SerializerWriteException;
import com.communote.plugins.export.serializer.StreamingSerializer;
import com.communote.plugins.export.types.User;
import com.communote.plugins.export.types.User.Status;
import com.communote.server.api.ServiceLocator;
import com.communote.server.core.ConfigurationManagement;
import com.communote.server.core.filter.ResultSpecification;
import com.communote.server.core.query.QueryManagement;
import com.communote.server.core.vo.query.QueryDefinitionRepository;
import com.communote.server.core.vo.query.QueryResultConverter;
import com.communote.server.core.vo.query.user.UserQuery;
import com.communote.server.core.vo.query.user.UserQueryParameters;
import com.communote.server.model.user.ExternalUserAuthentication;
import com.communote.server.model.user.UserProfile;
import com.communote.server.model.user.UserStatus;

/**
 * Exporter for the Communote users.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class UserExporter implements Exporter<User> {

    private class UserToExportableUserConverter extends
    QueryResultConverter<com.communote.server.model.user.User, User> {

        @Override
        public boolean convert(com.communote.server.model.user.User source, User target) {
            Status status = mapStatus(source.getStatus());
            if (status != null) {
                UserProfile profile = source.getProfile();
                target.setAlias(source.getAlias());
                target.setEmail(source.getEmail());
                target.setFirstname(profile.getFirstName());
                target.setId(source.getId());
                target.setLanguage(source.getLanguageLocale().getLanguage());
                target.setLastname(profile.getLastName());
                Set<ExternalUserAuthentication> auths = source.getExternalAuthentications();
                if (auths != null) {
                    for (ExternalUserAuthentication auth : auths) {
                        if (ConfigurationManagement.DEFAULT_LDAP_SYSTEM_ID.equals(auth
                                .getSystemId())) {
                            target.setLdapDN(auth.getAdditionalProperty());
                            target.setLdapExternalId(auth.getExternalUserId());
                            break;
                        }
                    }
                }
                target.setStatus(status);
                if (source.getTags() != null) {
                    target.setTags((new TagToStringConverter()).convert(source.getTags()));
                }
                return true;
            }
            LOGGER.warn("Skipping user {} because it has the wrong status {}", source.getId(),
                    source.getStatus().getValue());
            return false;
        }

        @Override
        public User create() {
            return new User();
        }

        private Status mapStatus(UserStatus kenmeiStatus) {
            Status mappedStatus;
            if (UserStatus.ACTIVE.equals(kenmeiStatus)) {
                mappedStatus = Status.ACTIVE;
            } else if (UserStatus.TEMPORARILY_DISABLED.equals(kenmeiStatus)) {
                mappedStatus = Status.DISABLED;
            } else if (UserStatus.PERMANENTLY_DISABLED.equals(kenmeiStatus)) {
                mappedStatus = Status.DELETED;
            } else {
                mappedStatus = null;
            }
            return mappedStatus;
        }

    }

    /**
     * number of elements to select with one query
     */
    private static int FETCH_AMOUNT = 30;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserExporter.class);

    private final List<Long> exportedUsers = new ArrayList<Long>();

    @Override
    public void export(StreamingSerializer<User> serializer) throws ExportFailedException {
        try {
            serializer.prepareSerialization();
            UserQuery userQuery = QueryDefinitionRepository.instance().getQueryDefinition(
                    UserQuery.class);
            UserQueryParameters queryParameters = userQuery.createInstance();
            queryParameters.setIncludeStatusFilter(new UserStatus[] {
                    UserStatus.ACTIVE, UserStatus.TEMPORARILY_DISABLED,
                    UserStatus.PERMANENTLY_DISABLED });
            UserToExportableUserConverter converter = new UserToExportableUserConverter();
            int offset = 0;
            while (true) {
                PageableList<User> result = findUsers(userQuery, queryParameters, offset, converter);
                for (User user : result) {
                    serializer.appendEntity(user);
                    exportedUsers.add(user.getId());
                }
                offset += result.size();
                if (result.getMinNumberOfAdditionalElements() == 0) {
                    break;
                }
            }
            serializer.finishSerialization();
        } catch (SerializerWriteException e) {
            throw new ExportFailedException("Exporting the users failed", e);
        }
    }

    private PageableList<User> findUsers(UserQuery userQuery, UserQueryParameters queryParameters,
            int offset, UserToExportableUserConverter converter) {
        queryParameters.setResultSpecification(new ResultSpecification(offset, FETCH_AMOUNT, 1));
        return ServiceLocator.findService(QueryManagement.class).query(userQuery, queryParameters,
                converter);
    }

    public List<Long> getExportedUsers() {
        return exportedUsers;
    }
}
