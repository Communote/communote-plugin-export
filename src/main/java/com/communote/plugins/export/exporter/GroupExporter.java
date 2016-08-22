package com.communote.plugins.export.exporter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.communote.common.util.PageableList;
import com.communote.plugins.export.serializer.SerializerWriteException;
import com.communote.plugins.export.serializer.StreamingSerializer;
import com.communote.plugins.export.types.Group;
import com.communote.server.api.ServiceLocator;
import com.communote.server.core.ConfigurationManagement;
import com.communote.server.core.filter.ResultSpecification;
import com.communote.server.core.query.QueryManagement;
import com.communote.server.core.vo.query.QueryResultConverter;
import com.communote.server.model.user.group.ExternalUserGroup;

/**
 * Exporter for all internal and external groups.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class GroupExporter implements Exporter<Group> {

    private class GroupConverter extends
            QueryResultConverter<com.communote.server.model.user.group.Group, Group> {

        @Override
        public boolean convert(com.communote.server.model.user.group.Group source, Group target) {
            target.setId(source.getId());
            target.setDescription(source.getDescription());
            target.setGroupName(source.getName());
            if (source instanceof ExternalUserGroup) {
                ExternalUserGroup extGroup = (ExternalUserGroup) source;
                if (ConfigurationManagement.DEFAULT_LDAP_SYSTEM_ID.equals(extGroup
                        .getExternalSystemId())) {
                    target.setLdapExternalId(extGroup.getExternalId());
                }
            }
            return true;
        }

        @Override
        public Group create() {
            return new Group();
        }

    }

    /**
     * number of elements to select with one query
     */
    private static int FETCH_AMOUNT = 30;

    private final Set<Long> exportedGroupIds = new HashSet<Long>();

    @Override
    public void export(StreamingSerializer<Group> serializer) throws ExportFailedException {
        try {
            serializer.prepareSerialization();
            GroupQuery query = new GroupQuery();
            GroupQueryParameters queryParameters = query.createInstance();
            GroupConverter converter = new GroupConverter();
            int offset = 0;
            while (true) {
                PageableList<Group> result = findGroups(query, queryParameters, offset, converter);
                for (Group group : result) {
                    exportedGroupIds.add(group.getId());
                    serializer.appendEntity(group);
                }
                offset += result.size();
                if (result.getMinNumberOfAdditionalElements() == 0) {
                    break;
                }
            }
            serializer.finishSerialization();
        } catch (SerializerWriteException e) {
            throw new ExportFailedException("Exporting the groups failed", e);
        }

    }

    private PageableList<Group> findGroups(GroupQuery query, GroupQueryParameters queryParameters,
            int offset, GroupConverter converter) {
        queryParameters.setResultSpecification(new ResultSpecification(offset, FETCH_AMOUNT, 1));
        return ServiceLocator.findService(QueryManagement.class).query(query, queryParameters,
                converter);
    }

    public Collection<Long> getExportedGroups() {
        return exportedGroupIds;
    }

}
