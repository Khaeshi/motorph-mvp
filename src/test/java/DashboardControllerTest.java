package com.motorph.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DashboardControllerTest {

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new DashboardController()).build();

    @Test
    void employeeDashboardEncodesUsername() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "xss<b>demo</b>", null, AuthorityUtils.createAuthorityList("ROLE_EMPLOYEE"));

        mockMvc.perform(get("/employee/dashboard").principal(auth))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("&lt;b&gt;demo&lt;/b&gt;")))
                .andExpect(content().string(not(containsString("<b>demo</b>"))));
    }
}