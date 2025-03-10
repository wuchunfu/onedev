package io.onedev.server.plugin.sso.openid;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;

import io.onedev.server.model.support.administration.sso.SsoAuthenticated;
import io.onedev.server.annotation.Editable;
import net.minidev.json.JSONObject;

@Editable(name="GitHub", order=100, description="Refer to this <a href='https://docs.onedev.io/tutorials/security/sso-with-github/' target='_blank'>tutorial</a> for an example setup")
public class GitHubConnector extends OpenIdConnector {

	private static final long serialVersionUID = 1L;

	public GitHubConnector() {
		setName("GitHub");
	}

	@Override
	public String getConfigurationDiscoveryUrl() {
		return super.getConfigurationDiscoveryUrl();
	}

	@Override
	protected ProviderMetadata discoverProviderMetadata() {
		return new ProviderMetadata(
				"https://github.com",
				"https://github.com/login/oauth/authorize", 
				"https://github.com/login/oauth/access_token", 
				"https://api.github.com/user");
	}

	@Override
	public String getButtonImageUrl() {
		ResourceReference logo = new PackageResourceReference(GitHubConnector.class, "octocat.png");
		return RequestCycle.get().urlFor(logo, new PageParameters()).toString();
	}

	@Override
	public String getGroupsClaim() {
		return super.getGroupsClaim();
	}

	@Override
	protected SsoAuthenticated processTokenResponse(OIDCTokenResponse tokenResponse) {
		BearerAccessToken accessToken = (BearerAccessToken) tokenResponse.getTokens().getBearerAccessToken();

		try {
			UserInfoRequest userInfoRequest = new UserInfoRequest(
					new URI(getCachedProviderMetadata().getUserInfoEndpoint()), accessToken);
			HTTPResponse httpResponse = userInfoRequest.toHTTPRequest().send();

			if (httpResponse.getStatusCode() == HTTPResponse.SC_OK) {
				JSONObject json = httpResponse.getContentAsJSONObject();
				String userName = (String) json.get("login");
				String email = (String) json.get("email");
				if (StringUtils.isBlank(email))
					throw new AuthenticationException("A public email is required");
				String fullName = (String) json.get("name");
				
				return new SsoAuthenticated(userName, email, fullName, null, null, this);
			} else {
				throw buildException(UserInfoErrorResponse.parse(httpResponse).getErrorObject());
			}
		} catch (SerializeException | ParseException | URISyntaxException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isManagingMemberships() {
		return false;
	}

}
