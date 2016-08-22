package com.communote.plugins.export.exporter;

import com.communote.server.core.vo.query.Query;
import com.communote.server.model.user.group.Group;
import com.communote.server.model.user.group.GroupConstants;

/**
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class GroupQuery extends Query<Group, GroupQueryParameters> {

    @Override
    public String buildQuery(GroupQueryParameters queryInstance) {
        return "from " + GroupConstants.CLASS_NAME;
    }

    @Override
    public GroupQueryParameters createInstance() {
        return new GroupQueryParameters();
    }

    @Override
    protected void setupQueries() {
    }

}
