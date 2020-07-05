package io.dropwizard.revolver;

import io.dropwizard.revolver.resource.RevolverRequestResource;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import jersey.repackaged.com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

/***
 User : nitish.goyal
 Date : 22/06/20
 Time : 6:58 PM
 ***/
@Slf4j
@WebServlet(name = "ApiServlet", urlPatterns = {"/apis/*"})
public class ApiServlet extends HttpServlet {

    private static final long serialVersionUID = -8387555735896795561L;
    private final RevolverRequestResource revolverRequestResource;

    public ApiServlet(RevolverRequestResource revolverRequestResource) {
        this.revolverRequestResource = revolverRequestResource;
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        HttpHeaders httpHeaders = getHeaders(req);
        String servicePath = (req.getPathInfo()
                .substring(req.getPathInfo()
                        .indexOf('/') + 1));
        String service = servicePath.substring(0, servicePath.indexOf('/'));
        String apiPath = servicePath.substring(servicePath.indexOf('/') + 1);
        UriInfo uriInfo = getUriInfo(req);
        Response response;
        try {
            response = revolverRequestResource.get(service, apiPath, httpHeaders, uriInfo);
        } catch (Exception e) {
            log.error("Error while making servlet call", e);
            resp.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return;
        }
        resp.setStatus(response.getStatus());
        resp.setContentType("application/json");
    }

    private UriInfo getUriInfo(HttpServletRequest req) {
        return new UriInfo() {
            @Override
            public String getPath() {
                return req.getPathInfo();
            }

            @Override
            public String getPath(boolean decode) {
                return null;
            }

            @Override
            public List<PathSegment> getPathSegments() {
                return null;
            }

            @Override
            public List<PathSegment> getPathSegments(boolean decode) {
                return null;
            }

            @Override
            public URI getRequestUri() {
                return null;
            }

            @Override
            public UriBuilder getRequestUriBuilder() {
                return null;
            }

            @Override
            public URI getAbsolutePath() {
                return null;
            }

            @Override
            public UriBuilder getAbsolutePathBuilder() {
                return null;
            }

            @Override
            public URI getBaseUri() {
                return null;
            }

            @Override
            public UriBuilder getBaseUriBuilder() {
                return null;
            }

            @Override
            public MultivaluedMap<String, String> getPathParameters() {
                return null;
            }

            @Override
            public MultivaluedMap<String, String> getPathParameters(boolean decode) {
                return null;
            }

            @Override
            public MultivaluedMap<String, String> getQueryParameters() {
                return null;
            }

            @Override
            public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
                return null;
            }

            @Override
            public List<String> getMatchedURIs() {
                return null;
            }

            @Override
            public List<String> getMatchedURIs(boolean decode) {
                return null;
            }

            @Override
            public List<Object> getMatchedResources() {
                return null;
            }

            @Override
            public URI resolve(URI uri) {
                return null;
            }

            @Override
            public URI relativize(URI uri) {
                return null;
            }
        };
    }

    private HttpHeaders getHeaders(HttpServletRequest req) {
        return new HttpHeaders() {
            @Override
            public List<String> getRequestHeader(String name) {
                return Lists.newArrayList(req.getHeader(name));
            }

            @Override
            public String getHeaderString(String name) {
                return req.getHeader(name);
            }

            @Override
            public MultivaluedMap<String, String> getRequestHeaders() {
                return new MultivaluedHashMap<>();
            }

            @Override
            public List<MediaType> getAcceptableMediaTypes() {
                return Lists.newArrayList(MediaType.APPLICATION_JSON_TYPE);
            }

            @Override
            public List<Locale> getAcceptableLanguages() {
                return null;
            }

            @Override
            public MediaType getMediaType() {
                return MediaType.APPLICATION_JSON_TYPE;
            }

            @Override
            public Locale getLanguage() {
                return null;
            }

            @Override
            public Map<String, Cookie> getCookies() {
                return null;
            }

            @Override
            public Date getDate() {
                return null;
            }

            @Override
            public int getLength() {
                return 0;
            }
        };
    }
}
