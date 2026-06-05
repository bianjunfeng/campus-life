package com.campus.campus_life_backend.modules.forum.controller;

import com.campus.campus_life_backend.common.config.JwtAuthenticationFilter;
import com.campus.campus_life_backend.common.config.SecurityConfig;
import com.campus.campus_life_backend.common.config.WebMvcSecurityConfig;
import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.ownership.ResourceOwnershipService;
import com.campus.campus_life_backend.common.security.service.AuthorizationService;
import com.campus.campus_life_backend.common.security.service.LoginPrincipalFactory;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.JwtUtil;
import com.campus.campus_life_backend.modules.auth.service.AuthSessionService;
import com.campus.campus_life_backend.modules.auth.service.TokenService;
import com.campus.campus_life_backend.modules.forum.entity.Post;
import com.campus.campus_life_backend.modules.forum.service.CategoryService;
import com.campus.campus_life_backend.modules.forum.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        WebMvcSecurityConfig.class,
        AuthorizationService.class,
        CurrentUserAccessor.class
})
class PostControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private AuthSessionService authSessionService;

    @MockitoBean
    private LoginPrincipalFactory loginPrincipalFactory;

    @MockitoBean
    private ResourceOwnershipService resourceOwnershipService;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void shouldRejectDeleteWhenRequesterIsNotOwner() throws Exception {
        stubAuthenticatedStudent("valid-token", Set.of("post:delete:self"));
        when(resourceOwnershipService.resolveOwnerId(ResourceTypeCode.POST, 88L)).thenReturn(2L);

        mockMvc.perform(delete("/api/forum/posts/88")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("无权访问该资源"));

        verifyNoInteractions(postService);
    }

    @Test
    void shouldAllowDeleteWhenRequesterOwnsPost() throws Exception {
        stubAuthenticatedStudent("valid-token", Set.of("post:delete:self"));
        when(resourceOwnershipService.resolveOwnerId(ResourceTypeCode.POST, 88L)).thenReturn(1L);
        Post post = new Post();
        post.setId(88L);
        when(postService.getPostById(88L)).thenReturn(post);

        mockMvc.perform(delete("/api/forum/posts/88")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("删除成功"));

        verify(postService).deletePost(88L);
    }

    private void stubAuthenticatedStudent(String token, Set<String> permissions) {
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getTokenType(token)).thenReturn("access");
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(tokenService.isAccessTokenValid(1L, token)).thenReturn(true);
        when(loginPrincipalFactory.create(1L))
                .thenReturn(new LoginPrincipal(1L, RoleCode.STUDENT, permissions));
    }
}
