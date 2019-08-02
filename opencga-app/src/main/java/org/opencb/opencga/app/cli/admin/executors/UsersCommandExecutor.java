/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.app.cli.admin.executors;


import org.apache.commons.lang3.StringUtils;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.opencga.app.cli.admin.AdminCliOptionsParser;
import org.opencb.opencga.catalog.db.api.UserDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.core.config.AuthenticationOrigin;
import org.opencb.opencga.core.models.User;
import org.opencb.opencga.core.results.LdapImportResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by imedina on 02/03/15.
 */
public class UsersCommandExecutor extends AdminCommandExecutor {

    private AdminCliOptionsParser.UsersCommandOptions usersCommandOptions;

    public UsersCommandExecutor(AdminCliOptionsParser.UsersCommandOptions usersCommandOptions) {
        super(usersCommandOptions.commonOptions);
        this.usersCommandOptions = usersCommandOptions;
    }

    @Override
    public void execute() throws Exception {
        logger.debug("Executing variant command line");

        String subCommandString = usersCommandOptions.getParsedSubCommand();
        switch (subCommandString) {
            case "create":
                create();
                break;
            case "import":
                importUsersAndGroups();
                break;
            case "sync":
                syncGroups();
                break;
            case "delete":
                delete();
                break;
            case "quota":
                setQuota();
                break;
            default:
                logger.error("Subcommand not valid");
                break;
        }
    }

    private void syncGroups() throws CatalogException {
        AdminCliOptionsParser.SyncCommandOptions executor = usersCommandOptions.syncCommandOptions;

        setCatalogDatabaseCredentials(executor.databaseHost, executor.prefix, executor.databaseUser, executor.databasePassword,
                executor.commonOptions.adminPassword);

        try (CatalogManager catalogManager = new CatalogManager(configuration)) {
            String sessionId = catalogManager.getUserManager().login("admin", configuration.getAdmin().getPassword());

            if (executor.syncAll) {
                catalogManager.getUserManager().syncAllUsersOfExternalGroup(executor.study, executor.authOrigin, sessionId);
            } else {
                catalogManager.getUserManager().importRemoteGroupOfUsers(executor.authOrigin, executor.from, executor.to, executor.study,
                        true, sessionId);
            }
        }
    }

    private void importUsersAndGroups() throws CatalogException {
        AdminCliOptionsParser.ImportCommandOptions executor = usersCommandOptions.importCommandOptions;

        // TODO: Remove this piece of code when we remove the deprecated variables
        if (StringUtils.isNotEmpty(executor.user)) {
            executor.id = executor.user;
            executor.resourceType = "user";
        } else if (StringUtils.isNotEmpty(executor.group)) {
            executor.id = executor.group;
            executor.resourceType = "group";
        }

        setCatalogDatabaseCredentials(executor.databaseHost, executor.prefix, executor.databaseUser, executor.databasePassword,
                executor.commonOptions.adminPassword);
        try (CatalogManager catalogManager = new CatalogManager(configuration)) {
            String token = catalogManager.getUserManager().login("admin", executor.commonOptions.adminPassword);

            if (StringUtils.isEmpty(executor.resourceType)) {
                logger.error("Missing resource type");
                return;
            }

            if ("user".equalsIgnoreCase(executor.resourceType) || "application".equalsIgnoreCase(executor.resourceType)) {
                catalogManager.getUserManager().importRemoteEntities(executor.authOrigin, Arrays.asList(executor.id.split(",")),
                        executor.resourceType.equalsIgnoreCase("application"), executor.studyGroup, executor.study, token);
            } else if ("group".equalsIgnoreCase(executor.resourceType)) {
                catalogManager.getUserManager().importRemoteGroupOfUsers(executor.authOrigin, executor.id, executor.studyGroup,
                        executor.study, false, token);
            } else {
                logger.error("Unknown resource type. Please use one of 'user', 'group' or 'application'");
            }
        }
    }

    private void printImportReport(LdapImportResult ldapImportResult) {
        if (ldapImportResult.getResult() != null) {
            LdapImportResult.SummaryResult userSummary = ldapImportResult.getResult().getUserSummary();
            if (userSummary != null) {
                if (userSummary.getNewUsers().size() > 0) {
                    System.out.println("New users registered: " + StringUtils.join(userSummary.getNewUsers(), ", "));
                }
                if (userSummary.getExistingUsers().size() > 0) {
                    System.out.println("Users already registered: " + StringUtils.join(userSummary.getExistingUsers(), ", "));
                }
                if (userSummary.getNonExistingUsers().size() > 0) {
                    System.out.println("Users not found in origin: " + StringUtils.join(userSummary.getNonExistingUsers(), ", "));
                }
            }
            List<String> usersInGroup = ldapImportResult.getResult().getUsersInGroup();
            if (usersInGroup != null) {
                System.out.println("Users registered in group " + ldapImportResult.getInput().getStudyGroup() + " from "
                        + ldapImportResult.getInput().getStudy() + ": " + StringUtils.join(usersInGroup, ", "));
            }
        }

        if (StringUtils.isNotEmpty(ldapImportResult.getWarningMsg())) {
            System.out.println("WARNING: " + ldapImportResult.getWarningMsg());
        }

        if (StringUtils.isNotEmpty(ldapImportResult.getErrorMsg())) {
            System.out.println("ERROR: " + ldapImportResult.getErrorMsg());
        }
    }

    private void create() throws CatalogException, IOException {
        setCatalogDatabaseCredentials(usersCommandOptions.createUserCommandOptions.databaseHost,
                usersCommandOptions.createUserCommandOptions.prefix, usersCommandOptions.createUserCommandOptions.databaseUser,
                usersCommandOptions.createUserCommandOptions.databasePassword,
                usersCommandOptions.createUserCommandOptions.commonOptions.adminPassword);

        long userQuota;
        if (usersCommandOptions.createUserCommandOptions.userQuota != null) {
            userQuota = usersCommandOptions.createUserCommandOptions.userQuota;
        } else {
            userQuota = configuration.getUserDefaultQuota();
        }

        try (CatalogManager catalogManager = new CatalogManager(configuration)) {
            catalogManager.getUserManager().login("admin", configuration.getAdmin().getPassword());

            User user = catalogManager.getUserManager().create(usersCommandOptions.createUserCommandOptions.userId,
                    usersCommandOptions.createUserCommandOptions.userName, usersCommandOptions.createUserCommandOptions.userEmail,
                    usersCommandOptions.createUserCommandOptions.userPassword,
                    usersCommandOptions.createUserCommandOptions.userOrganization, userQuota,
                    usersCommandOptions.createUserCommandOptions.type, null).first();

            System.out.println("The user has been successfully created: " + user.toString() + "\n");
        }
    }

    private void delete() throws CatalogException, IOException {
        setCatalogDatabaseCredentials(usersCommandOptions.deleteUserCommandOptions.databaseHost,
                usersCommandOptions.deleteUserCommandOptions.prefix, usersCommandOptions.deleteUserCommandOptions.databaseUser,
                usersCommandOptions.deleteUserCommandOptions.databasePassword,
                usersCommandOptions.deleteUserCommandOptions.commonOptions.adminPassword);

        try (CatalogManager catalogManager = new CatalogManager(configuration)) {
            catalogManager.getUserManager().login("admin", configuration.getAdmin().getPassword());

            List<QueryResult<User>> deletedUsers = catalogManager.getUserManager()
                    .delete(usersCommandOptions.deleteUserCommandOptions.userId, new QueryOptions("force", true), null);
            for (QueryResult<User> deletedUser : deletedUsers) {
                User user = deletedUser.first();
                if (user != null) {
                    System.out.println("The user has been successfully deleted from the database: " + user.toString());
                } else {
                    System.out.println(deletedUser.getErrorMsg());
                }
            }
        }
    }

    private void setQuota() throws CatalogException {
        setCatalogDatabaseCredentials(usersCommandOptions.QuotaUserCommandOptions.databaseHost,
                usersCommandOptions.QuotaUserCommandOptions.prefix, usersCommandOptions.QuotaUserCommandOptions.databaseUser,
                usersCommandOptions.QuotaUserCommandOptions.databasePassword,
                usersCommandOptions.QuotaUserCommandOptions.commonOptions.adminPassword);

        try (CatalogManager catalogManager = new CatalogManager(configuration)) {
            catalogManager.getUserManager().login("admin", configuration.getAdmin().getPassword());

            User user = catalogManager.getUserManager().update(usersCommandOptions.QuotaUserCommandOptions.userId, new ObjectMap
                    (UserDBAdaptor.QueryParams.QUOTA.key(), usersCommandOptions.QuotaUserCommandOptions.quota * 1073741824), null, null).first();

            System.out.println("The disk quota has been properly updated: " + user.toString());
        }
    }

    private AuthenticationOrigin getAuthenticationOrigin(String authOrigin) {
        if (configuration.getAuthentication().getAuthenticationOrigins() != null) {
            for (AuthenticationOrigin authenticationOrigin : configuration.getAuthentication().getAuthenticationOrigins()) {
                if (authOrigin.equals(authenticationOrigin.getId())) {
                    return authenticationOrigin;
                }
            }
        }
        return null;
    }

}
