package org.javlo.helper;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

import org.javlo.context.ContentContext;
import org.javlo.context.GlobalContext;
import org.javlo.navigation.MenuElement;
import org.javlo.security.password.IPasswordEncryption;
import org.javlo.service.visitors.UserDataService;
import org.javlo.user.AdminUserSecurity;
import org.javlo.user.User;
import org.javlo.user.UserFactory;

public class SecurityHelper {
	
	public static final String USER_CODE_KEY = "__userCode";

	private static Logger logger = Logger.getLogger(SecurityHelper.class.getName());
	
	public static IPasswordEncryption passwordEncrypt = null;

	public static boolean userAccessPage(ContentContext ctx, User user, MenuElement page) {
		if (AdminUserSecurity.getInstance().isAdmin(user)) {
			return true;
		} else if (user != null && !Collections.disjoint(page.getUserRoles(), user.getRoles())) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean checkUserAccess(ContentContext ctx) {
		if (ctx.getCurrentEditUser() != null) {
			String userContextName = ctx.getCurrentEditUser().getContext();
			String currentContextName = ctx.getGlobalContext().getContextKey();
			if (userContextName != null && !userContextName.equals(currentContextName)) {
				GlobalContext userContext;
				try {
					userContext = GlobalContext.getInstance(ctx.getRequest().getSession(), ctx.getCurrentEditUser().getContext());
					if (!userContext.isMaster()) {
						logger.info("logout user : " + ctx.getCurrentEditUser().getLogin() + " because context does'nt match (" + userContextName + " != " + currentContextName + ')');
						UserFactory.createUserFactory(ctx.getRequest()).logout(ctx.getRequest().getSession());						
						ctx.setNeedRefresh(true);
						return false;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	
	public static final String encryptPassword(String pwd) {
		return passwordEncrypt.encrypt(pwd);
	}
	
	public String anonymisedIp(String ip) {
		return ip;
	}
	
	public static void clearUserCode(ContentContext ctx) {
		ctx.getRequest().getSession().removeAttribute(USER_CODE_KEY);
	}
	
	public static String getUserCode(ContentContext ctx) throws Exception {
		String userCode = null;
		if (ctx.getRequest().getSession().getAttribute(USER_CODE_KEY) != null) {
			userCode = ctx.getRequest().getSession().getAttribute(USER_CODE_KEY).toString();
		}
		UserDataService userDataService = UserDataService.getInstance(ctx);
		if (userDataService.getUserData(ctx, USER_CODE_KEY) != null) {
			userCode = userDataService.getUserData(ctx, USER_CODE_KEY);
		} else {
			if (userCode == null) {
				userCode = StringHelper.getLargeRandomIdBase64();
				 userDataService.addUserData(ctx, USER_CODE_KEY, userCode);
			}
		}
		ctx.getRequest().getSession().setAttribute(USER_CODE_KEY, userCode);
		return userCode;
	}
}
