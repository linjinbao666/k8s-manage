package ecs.vo;

import lombok.ToString;

import javax.servlet.http.Cookie;


@ToString
public class UserContext {
	private static ThreadLocal<UserContext> context = new ThreadLocal<UserContext>();

	private UserInfo user;
	private Cookie[] cookies;
	public static UserContext getUserContext() {
		UserContext userContext = context.get();
		if (userContext == null) {
			context.set(new UserContext());
		}
		return context.get();
	}

	public static void setUserContext(UserContext userContext) {
		context.set(userContext);
	}

	public UserInfo getUser() {
		if (user == null) {
			user = new UserInfo();
		}
		return user;
	}

	public void setUser(UserInfo user) {
		this.user = user;
	}

	public Cookie[] getCookies() {
		if (cookies == null) {
			cookies = new Cookie[] {};
		}
		return cookies;
	}

	public void setCookies(Cookie[] cookies) {
		this.cookies = cookies;
	}

}
