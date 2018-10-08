package com.app.filters;


import java.io.IOException;
import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import javax.ws.rs.Priorities;

import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;
import com.app.model.user.*;
import com.app.model.BaseResponse;
import com.app.model.BaseResponse.MessageTypeEnum;
import com.app.util.TokenUtil;
import com.app.util.Constants.UserRoleConstants;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthorizationRequestFilter implements ContainerRequestFilter {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(AuthorizationRequestFilter.class);

    @Context
    HttpServletRequest request;

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext reqContext ) throws IOException {
        Method method = resourceInfo.getResourceMethod();
        String className = method.getDeclaringClass().getName();
        String path = reqContext.getUriInfo().getPath();
        BaseResponse resp = new BaseResponse();
        final UserViewModel userView;

        //Allow Access for @PermitAll or SwagerSpec
        if (reqContext.getMethod().equalsIgnoreCase("OPTIONS") ||  method == null || method.isAnnotationPresent(PermitAll.class) || className.equals("io.swagger.jaxrs.listing.ApiListingResource")) {
            return;
        }

        String jwtToken = reqContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isBlank(jwtToken)) {
            resp.setTypeAndMessage(MessageTypeEnum.BAD_TOKEN, "Empty Token" );
            reqContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON).entity(resp).build());
            return;
        }

        try {
            userView = TokenUtil.getUserFromToken(jwtToken);
            if (userView==null) {
                resp.setTypeAndMessage(MessageTypeEnum.BAD_TOKEN, "Invalid Token" );
                reqContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON).entity(resp).build());
                return;
            }
        }
        catch (Exception e) {
            log.error("Invalid user token " + e.getMessage());
            resp.setTypeAndMessage(MessageTypeEnum.BAD_TOKEN, "Invalid User" );
            reqContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON).entity(resp).build());
            return;
        }

        // Store Account in the SecurityContext
        final SecurityContext securityContext = reqContext.getSecurityContext();
        reqContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {return userView;}

            @Override
            public boolean isUserInRole(String role) {return securityContext.isUserInRole(role);}

            @Override
            public boolean isSecure() {return securityContext.isSecure();}

            @Override
            public String getAuthenticationScheme() {return "Token-Based-Auth-Scheme";}
        });


        // Everything is permitted for the role "admin"
        if (userView.getRole().equalsIgnoreCase(UserRoleConstants.ROLE_ADMIN)){
            return;
        }

        //check for roles
        if(method.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
            Set<String> allowedRoleSet = new HashSet<>(Arrays.asList(rolesAnnotation.value() ));
            if (allowedRoleSet.contains(userView.getRole())){
                return;
            }
            if (allowedRoleSet.contains(UserRoleConstants.ROLE_CUSTOMER)){
                // Any endpoint for role "CUSTOMER" is available to all authenticated users (ADMIN, SUPPORT)
                return;
            }
            else{
                resp.setTypeAndMessage(MessageTypeEnum.NO_ACCESS, "Unauthorized for " +  userView.getRole() + " role");
                reqContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(resp).build());
                return;
            }
        }
    }

}



