/*
 * Created on 19-sept.-2003
 */
package org.javlo.component.links;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.javlo.component.core.ComplexPropertiesLink;
import org.javlo.component.core.ComponentBean;
import org.javlo.component.core.ComponentLayout;
import org.javlo.component.core.IInternalLink;
import org.javlo.component.core.IReverseLinkComponent;
import org.javlo.context.ContentContext;
import org.javlo.context.GlobalContext;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.helper.XHTMLHelper;
import org.javlo.i18n.I18nAccess;
import org.javlo.message.GenericMessage;
import org.javlo.module.mailing.MailingAction;
import org.javlo.navigation.MenuElement;
import org.javlo.navigation.PageBean;
import org.javlo.service.ContentService;
import org.javlo.service.NavigationService;
import org.javlo.service.RequestService;
import org.javlo.service.ReverseLinkService;
import org.javlo.service.exception.ServiceException;

/**
 * @author pvandermaesen
 */
public class SimpleInternalLink extends ComplexPropertiesLink implements IInternalLink, IReverseLinkComponent {

	/**
	 * create a static logger.
	 */
	protected static Logger logger = Logger.getLogger(SimpleInternalLink.class.getName());

	private static final String REVERSE_LINK_KEY = "reverse-link";

	public static final String TYPE = "simple-internal-link";

	@Override
	public void init(ComponentBean bean, ContentContext newContext) throws Exception {
		super.init(bean, newContext);
		properties.load(stringToStream(getValue()));
		if (getLayout() == null) {
			getComponentBean().setLayout(new ComponentLayout(""));
		}
	}

	@Override
	public boolean isInline() {
		return true;
	}

	@Override
	protected String getTag(ContentContext ctx) {
		String style = getStyle();
		if (style != null) {
			for (int i = 2; i < 7; i++) {
				if (style.toLowerCase().contains("h" + i)) {
					return "h" + i;
				}
			}
		}
		return super.getTag(ctx);
	}

	@Override
	public void prepareView(ContentContext ctx) throws Exception {
		super.prepareView(ctx);
		String linkId = properties.getProperty(LINK_KEY, "/");

		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		NavigationService navigationService = NavigationService.getInstance(globalContext);
		MenuElement child = navigationService.getPage(ctx, linkId);
		if (child != null) {
			ctx.getRequest().setAttribute("page", new PageBean(ctx, child));
			String link = "#";
			link = child.getPath();
			String label = properties.getProperty(LABEL_KEY, "");
			if (label.trim().length() == 0) {
				label = child.getLabel(ctx);
			}
			ctx.getRequest().setAttribute("label", label);
			String url = URLHelper.createURL(ctx, link);
			if (ctx.getRenderMode() != ContentContext.PAGE_MODE) {
				ctx.getRequest().setAttribute("url", StringHelper.toXMLAttribute(url));
			} else {
				ContentContext viewCtx = new ContentContext(ctx);
				viewCtx.setRenderMode(ContentContext.VIEW_MODE);
				url = URLHelper.createURL(viewCtx, link);
				if (getParam().trim().length() == 0) {
					ctx.getRequest().setAttribute("url",
							StringHelper.toXMLAttribute(url) + "?" + MailingAction.MAILING_FEEDBACK_PARAM_NAME + "="
									+ MailingAction.MAILING_FEEDBACK_VALUE_NAME);
				} else {
					ctx.getRequest().setAttribute("url",
							StringHelper.toXMLAttribute(url) + getParam() + "&"
									+ MailingAction.MAILING_FEEDBACK_PARAM_NAME + "="
									+ MailingAction.MAILING_FEEDBACK_VALUE_NAME);
				}
			}
		}
	}

	@Override
	protected String getForcedPrefixViewXHTMLCode(ContentContext ctx) {
		if (getConfig(ctx).getProperty("prefix", null) != null) {
			return getConfig(ctx).getProperty("prefix", null);
		}
		String style = contructViewStyle(ctx);
		String prefix = "";
		if (getComponentBean().isList()) {
			prefix = "<li>";
		}
		if (style == null) {
			style = getType();
		} else {
			style = style + ' ' + getType();
		}

		String linkId = properties.getProperty(LINK_KEY, "/");

		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		NavigationService navigationService;
		try {
			navigationService = NavigationService.getInstance(globalContext);
			MenuElement child = navigationService.getPage(ctx, linkId);
			if (child != null) {
				String link = child.getPath();
				String label = properties.getProperty(LABEL_KEY, "");
				if (label.trim().length() == 0) {
					label = child.getLabel(ctx);
				}
				String url = URLHelper.createURL(ctx, link);
				prefix = prefix + "<a" + getInlineStyle(ctx) + ' '
						+ getSpecialPreviewCssClass(ctx, style + ' ' + getStyle(ctx)) + getSpecialPreviewCssId(ctx)
						+ " href=\"";
				if (ctx.getRenderMode() != ContentContext.PAGE_MODE) {
					prefix = prefix + StringHelper.toXMLAttribute(url);
				} else {
					ContentContext viewCtx = new ContentContext(ctx);
					viewCtx.setRenderMode(ContentContext.VIEW_MODE);
					url = URLHelper.createURL(viewCtx, link);
					if (getParam().trim().length() == 0) {
						prefix = prefix + StringHelper.toXMLAttribute(url) + "?"
								+ MailingAction.MAILING_FEEDBACK_PARAM_NAME + "="
								+ MailingAction.MAILING_FEEDBACK_VALUE_NAME;
					} else {
						prefix = prefix + StringHelper.toXMLAttribute(url) + getParam() + "&"
								+ MailingAction.MAILING_FEEDBACK_PARAM_NAME + "="
								+ MailingAction.MAILING_FEEDBACK_VALUE_NAME;
					}
				}
				prefix = prefix + "\">";
			}
			if (isAjaxWrapper(ctx)) {
				prefix = prefix + "<div id=\"" + getAjaxId() + "\">";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return prefix;
	}

	@Override
	public String getSuffixViewXHTMLCode(ContentContext ctx) {
		if (isWrapped(ctx)) {
			if (getComponentBean().isList()) {
				return "</a></li>";
			} else {
				return "</a>";
			}
		} else {
			return "";
		}
	}

	/**
	 * @see org.javlo.itf.IContentVisualComponent#getXHTMLCode()
	 */
	@Override
	public String getViewXHTMLCode(ContentContext ctx) throws Exception {
		NavigationService navigationService = NavigationService.getInstance(ctx.getGlobalContext());
		MenuElement child = navigationService.getPage(ctx, properties.getProperty(LINK_KEY, "/"));
		String label = properties.getProperty(LABEL_KEY, "");
		if (label.trim().length() == 0) {
			label = child.getLabel(ctx);
		}
		return "<div " + getInlineStyle(ctx) + " class=\"label\">" + label + "</div>";
	}

	protected String getParam() throws Exception {
		return "";
	}

	@Override
	public String getLinkId() {
		return properties.getProperty(LINK_KEY, null);
	}

	public String getLabel() {
		return properties.getProperty(LABEL_KEY, "");
	}

	public String getReverseLinkName() {
		return getId() + ID_SEPARATOR + "reverse-lnk";
	}

	@Override
	protected String getEditXHTMLCode(ContentContext ctx) throws Exception {

		ContentService content = ContentService.getInstance(ctx.getRequest());

		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);

		String linkIdStr = properties.getProperty(LINK_KEY, null);
		String label = getLabel();

		String link = "/";
		if (linkIdStr != null) {
			GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
			NavigationService navigationService = NavigationService.getInstance(globalContext);
			MenuElement elemChild = navigationService.getPage(ctx, linkIdStr);
			if (elemChild != null) {
				link = elemChild.getPath();
			}
		}

		try {
			I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());
			String linkTitle = i18nAccess.getText("component.link.link");
			String labelTitle = i18nAccess.getText("component.link.label");
			String reverseLinkLabel = i18nAccess.getText("component.link.reverse");

			String reverseLink = properties.getProperty(REVERSE_LINK_KEY, null);
			if (reverseLink == null) {
				reverseLink = "none";
			}
			out.println("<div class=\"form-group\"><div class=\"row\"><div class=\"col-sm-4\">");
			out.println("<label for=\"" + getReverseLinkName() + "\">" + reverseLinkLabel
					+ " : </label></div><div class=\"col-sm-8\">");
			out.println(XHTMLHelper.getReverlinkSelectType(ctx, getReverseLinkName(), reverseLink));

			out.println("</div></div></div>");
			out.println("<div class=\"form-group\"><div class=\"row\"><div class=\"col-sm-4\">");

			out.println(
					"<label for=\"" + getLinkName() + "\">" + linkTitle + " : </label></div><div class=\"col-sm-6\">");
			out.println("<select class=\"form-control\" id=\"" + getLinkName() + "\" name=\"" + getLinkName() + "\">");
			MenuElement elem = content.getNavigation(ctx);
			String[] values = elem.getChildList();
			String currentLink = null;
			for (String value : values) {
				if (link.equals(value)) {
					currentLink = value;
					out.println("<option selected=\"selected\" value=\"" + value + "\">");
				} else {
					out.println("<option value=\"" + value + "\">");
				}
				out.println(value);
				out.println("</option>");
			}
			out.println("</select></div><div class=\"col-sm-2\">");
			if (currentLink != null) {
				out.print("<a href=\"");
				out.print(URLHelper.createURL(ctx, currentLink));
				out.println("\">&nbsp;&gt;&gt;</a>");
				setMessage(null);
			} else {
				setMessage(new GenericMessage(i18nAccess.getText("component.message.help.choose_link"),
						GenericMessage.HELP));
			}
			out.println("</div></div></div>");
			out.println("<div class=\"form-group\"><div class=\"row\"><div class=\"col-sm-4\">");

			out.println("<label for=\"" + getLinkLabelName() + "\">" + labelTitle
					+ " : </label></div><div class=\"col-sm-8\">");
			out.println(XHTMLHelper.getTextInput(getLinkLabelName(), label, "form-control"));
			out.println("</div></div></div>");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return writer.toString();
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String performEdit(ContentContext ctx) {

		ContentService content = ContentService.getInstance(ctx.getRequest());

		RequestService requestService = RequestService.getInstance(ctx.getRequest());

		String label = requestService.getParameter(getLinkLabelName(), null);
		String link = requestService.getParameter(getLinkName(), "/");

		if (label != null) {

			try {
				MenuElement elem = content.getNavigation(ctx).searchRealChild(ctx, link);
				String idLink = "";
				if (elem != null) {
					idLink = elem.getId();
				} else {
					logger.warning("link not found : " + link);
				}
				String linkIdStr = properties.getProperty(LINK_KEY, "");
				String oldLabel = getLabel();
				if ((!label.equals(oldLabel)) || (!linkIdStr.equals(idLink))) {
					if (linkIdStr.trim().length() == 0) {
						setNeedRefresh(true);
					}
					setModify();
					properties.setProperty(LINK_KEY, idLink);
					properties.setProperty(LABEL_KEY, label);
				}
				String reverseLinkValue = requestService.getParameter(getReverseLinkName(), null);
				if (reverseLinkValue != null
						&& !properties.getProperty(REVERSE_LINK_KEY, "").equals(reverseLinkValue)) {
					properties.setProperty(REVERSE_LINK_KEY, reverseLinkValue);
					setModify();
					GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
					ReverseLinkService reverselinkService = ReverseLinkService.getInstance(globalContext);
					reverselinkService.clearCache();
				}
				if (isModify()) {
					storeProperties();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public String getHexColor() {
		return LINK_COLOR;
	}

	@Override
	public boolean isListable() {
		return true;
	}

	@Override
	public String getHelpURI(ContentContext ctx) {
		return "/components/internlink.html";
	}

	@Override
	public String getLinkText(ContentContext ctx) {
		try {
			return getLabel();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public String getLinkURL(ContentContext ctx) {
		String linkId = properties.getProperty(LINK_KEY, "/");
		NavigationService navigationService;
		try {
			GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
			navigationService = NavigationService.getInstance(globalContext);
			MenuElement child = navigationService.getPage(ctx, linkId);
			if (child != null) {
				String link = "#";
				link = child.getPath();
				return URLHelper.createURL(ctx, link);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public boolean isReverseLink() {
		boolean outIsReverseLink = !(properties.getProperty(REVERSE_LINK_KEY, "none").equals("none")
				|| properties.getProperty(REVERSE_LINK_KEY, "").equals(""));
		return outIsReverseLink;
	}

	@Override
	public boolean isOnlyFirstOccurrence() {
		return properties.getProperty(REVERSE_LINK_KEY, "none").equals(ReverseLinkService.ONLY_FIRST);
	}

	@Override
	public boolean isOnlyThisPage() {
		return properties.getProperty(REVERSE_LINK_KEY, "none").equals(ReverseLinkService.ONLY_THIS_PAGE);
	}

	@Override
	public boolean isOnlyPreviousComponent() {
		return properties.getProperty(REVERSE_LINK_KEY, "none").equals(ReverseLinkService.ONLY_PREVIOUS_COMPONENT);
	}

	@Override
	public boolean initContent(ContentContext ctx) throws Exception {
		properties.setProperty(LINK_KEY, "0");
		properties.setProperty(LABEL_KEY, getType());
		return true;
	}

	@Override
	public String getListGroup() {
		return "link";
	}

	@Override
	public String getFontAwesome() {
		return "link";
	}

}
