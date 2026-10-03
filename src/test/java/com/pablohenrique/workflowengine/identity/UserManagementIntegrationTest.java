package com.pablohenrique.workflowengine.identity;

import com.pablohenrique.workflowengine.support.AbstractIntegrationTest;
import com.pablohenrique.workflowengine.support.BrowserSession;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserManagementIntegrationTest extends AbstractIntegrationTest {

    private static final String USERS = "/api/v1/users";
    private static final String PASSWORD = "a-strong-passphrase";

    private static String newUsername() {
        return "u" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String createUser(String username, String roles) throws Exception {
        postJson(USERS, admin(), """
                {"username": "%s", "displayName": "Bruna Costa", "password": "%s", "roles": [%s]}
                """.formatted(username, PASSWORD, roles)).andExpect(status().isCreated());
        return username;
    }

    @Test
    void administratorsCreateAccountsWithoutEverExposingPasswords() throws Exception {
        String username = newUsername();

        postJson(USERS, admin(), """
                {"username": "%s", "displayName": "Bruna Costa", "password": "%s", "roles": ["USER", "FINANCE"]}
                """.formatted(username, PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(USERS + "/" + username)))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.roles", contains("FINANCE", "USER")))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        get_(USERS + "?size=20&sort=createdAt,desc", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username", hasItem(username)));
        get_(AUDIT + "?resourceId=" + username + "&operation=USER_CREATED", admin())
                .andExpect(jsonPath("$.content[0].actorId").value("admin"));
    }

    @Test
    void rejectsDuplicatesWeakPasswordsAndInvalidData() throws Exception {
        String username = createUser(newUsername(), "\"USER\"");

        postJson(USERS, admin(), """
                {"username": "%s", "displayName": "Dup", "password": "%s", "roles": ["USER"]}
                """.formatted(username, PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
        postJson(USERS, admin(), """
                {"username": "%s", "displayName": "Weak", "password": "12345", "roles": ["USER"]}
                """.formatted(newUsername()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.violations[0]").value("password must have at least 12 characters"));
        postJson(USERS, admin(), """
                {"username": "Not Valid", "displayName": "X", "password": "%s", "roles": ["lowercase"]}
                """.formatted(PASSWORD))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Invalid user"));
        postJson(USERS, admin(), "{\"username\": \"x\"}").andExpect(status().isBadRequest());
        get_(USERS + "/" + newUsername(), admin()).andExpect(status().isNotFound());
    }

    @Test
    void onlyAdministratorsManageAccounts() throws Exception {
        get_(USERS, manager()).andExpect(status().isForbidden());
        postJson(USERS, user(), "{}").andExpect(status().isForbidden());
    }

    @Test
    void changingAccessEndsTheUserSessionsImmediately() throws Exception {
        String username = createUser(newUsername(), "\"USER\"");
        BrowserSession browser = new BrowserSession(mockMvc, "10.9.9.1");
        browser.login(username, PASSWORD).andExpect(status().isOk());
        browser.perform(get("/api/v1/auth/me")).andExpect(status().isOk());

        mockMvc.perform(put(USERS + "/" + username).with(admin()).contentType("application/json")
                        .content("{\"displayName\": \"Bruna C.\", \"roles\": [\"USER\", \"MANAGER\"], \"enabled\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", contains("MANAGER", "USER")));

        browser.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        BrowserSession again = new BrowserSession(mockMvc, "10.9.9.2");
        again.login(username, PASSWORD).andExpect(jsonPath("$.roles", contains("MANAGER", "USER")));
    }

    @Test
    void disabledAccountsCannotAuthenticate() throws Exception {
        String username = createUser(newUsername(), "\"USER\"");

        mockMvc.perform(put(USERS + "/" + username).with(admin()).contentType("application/json")
                        .content("{\"displayName\": \"Bruna\", \"roles\": [\"USER\"], \"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        new BrowserSession(mockMvc, "10.9.9.3").login(username, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void administratorsCannotLockThemselvesOut() throws Exception {
        String otherAdmin = createUser(newUsername(), "\"ADMIN\", \"USER\"");

        mockMvc.perform(put(USERS + "/admin").with(admin()).contentType("application/json")
                        .content("{\"displayName\": \"Admin\", \"roles\": [\"USER\"], \"enabled\": true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_LOCKOUT"));
        mockMvc.perform(put(USERS + "/" + otherAdmin).with(admin()).contentType("application/json")
                        .content("{\"displayName\": \"Ex-admin\", \"roles\": [\"USER\"], \"enabled\": true}"))
                .andExpect(status().isOk());
    }

    @Test
    void passwordResetByAnAdministratorReplacesTheCredential() throws Exception {
        String username = createUser(newUsername(), "\"USER\"");

        postJson(USERS + "/" + username + "/password", admin(), "{\"newPassword\": \"short\"}")
                .andExpect(status().isUnprocessableContent());
        postJson(USERS + "/" + username + "/password", admin(), "{\"newPassword\": \"a-brand-new-passphrase\"}")
                .andExpect(status().isNoContent());

        new BrowserSession(mockMvc, "10.9.9.4").login(username, PASSWORD).andExpect(status().isUnauthorized());
        new BrowserSession(mockMvc, "10.9.9.5").login(username, "a-brand-new-passphrase").andExpect(status().isOk());
        get_(AUDIT + "?resourceId=" + username + "&operation=USER_PASSWORD_RESET", admin())
                .andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"));
    }
}
