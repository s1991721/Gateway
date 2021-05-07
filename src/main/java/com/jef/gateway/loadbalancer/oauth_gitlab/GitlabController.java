package com.jef.gateway.loadbalancer.oauth_gitlab;

import lombok.extern.slf4j.Slf4j;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * @Author: Jef
 * @Date: 2021/5/7 14:42
 * @Description gitlab sso
 */
@Controller
@Slf4j
public class GitlabController {

    private String gitlabServerUrl = "http://localhost";

    private String authorizePath = "/oauth/authorize";
    private String tokenPath = "/oauth/token";

    private String clientId = "4ecbaacb7b0c24ff160d3d1119c0981a08dfe2122453298db930f29f4577818b";
    private String clientSecret = "4bf9f7203d4de8d83f6c03a69485c8dcce565a05f23188799fcc29719427aceb";
    private String callbackUrl = "http://localhost:8080/callback";


    OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

    String currentUserMock = "yunshi";

    HashMap<String, String> tokenRepository = new HashMap<>();

    @PreDestroy
    public void cleanUp() {
        oAuthClient.shutdown();
    }

    @RequestMapping("/main")
    @ResponseBody
    public String main() {
        if (!ObjectUtils.isEmpty(tokenRepository.get(currentUserMock))) {
            return "authorization is done, you are good to go with access token: " + tokenRepository.get(currentUserMock);
        } else {
            return "no authority.";
        }
    }

    @RequestMapping("/")
    public String index(HttpServletRequest req, HttpServletResponse response) throws Throwable {
        if (!ObjectUtils.isEmpty(tokenRepository.get(currentUserMock))) {
            log.info("query user information with access token...");
            OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(gitlabServerUrl + "/api/v3/user").setAccessToken(tokenRepository.get(currentUserMock)).buildQueryMessage();
            OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);
            log.info("had authorized, just query for user information...");
            log.info("user information: " + resourceResponse.getBody());
            return "redirect:/main";
        } else {
            log.info("first login, build oauth request >..");
            OAuthClientRequest request = OAuthClientRequest
                    .authorizationLocation(gitlabServerUrl + authorizePath)
                    .setClientId(clientId)
                    .setRedirectURI(callbackUrl)
                    .setResponseType("code")
                    .buildQueryMessage();

            String gitlabAuthUrl = request.getLocationUri();

            log.info("redirect to : " + gitlabAuthUrl);
            return "redirect:" + gitlabAuthUrl;
        }
    }


    @RequestMapping("/callback")
    public String callback(@RequestParam(value = "code", required = false) String code,
                           @RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "error_description", required = false) String errorDescription) throws Throwable {

        if (StringUtils.hasLength(error)) {
            log.error("authorization fails with error={} and error description={}", error, errorDescription);
        } else {
            log.info("callback request receives with code={}", code);

            OAuthClientRequest request = OAuthClientRequest
                    .tokenLocation(gitlabServerUrl + tokenPath)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRedirectURI(callbackUrl)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setCode(code)
                    .buildQueryMessage();

            log.info("build authorize request with code:{} and client secret", code);

            OAuthJSONAccessTokenResponse response = oAuthClient.accessToken(request);
            String accessToken = response.getAccessToken();
            log.info("access token got: {}", accessToken);

            // save access token for further use, then redirect user to another url in our own application.
            tokenRepository.put(currentUserMock, accessToken);
        }

        return "redirect:/main";
    }
}
