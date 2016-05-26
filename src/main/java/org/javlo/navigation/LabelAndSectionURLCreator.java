package org.javlo.navigation;

import java.net.URLEncoder;
import java.util.Collection;

import org.javlo.component.core.IContentVisualComponent;
import org.javlo.component.navigation.PageURL;
import org.javlo.context.ContentContext;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;

/**
 * create url based on the title of the page.
 * 
 * @author Patrick Vandermaesen
 * 
 */
public class LabelAndSectionURLCreator extends AbstractURLFactory {

	/**
	 * return the name of the first level page active. "root" if current page in
	 * root.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected MenuElement getSectionPage(MenuElement page) {		
		if (page == null) {
			return null;
		}
		if (page.getParent() == null || page.getParent().getParent() == null) {
			return null;
		} else {
			while (page.getParent().getParent() != null) {
				page = page.getParent();
			}
		}
		return page;
	}

	protected String createURLWithoutExt(ContentContext ctx, MenuElement currentPage) throws Exception {

		if (currentPage == null) {
			return "/";
		}

		ContentContext freeCtx = ctx.getFreeContentContext();
		ContentContext realContentContext = freeCtx.getContextWithContent(currentPage);
		if (realContentContext != null) {
			freeCtx = realContentContext;
		}
		
		Collection<IContentVisualComponent> comps = currentPage.getContentByType(freeCtx, PageURL.TYPE);
		if (comps.size() > 0) {
			return ((PageURL) comps.iterator().next()).getValue();
		}
		
		String label;
		String pageTitle = currentPage.getForcedPageTitle(freeCtx);
		
		if (!StringHelper.isEmpty(pageTitle)) {
			label = pageTitle;
		} else {
			label = currentPage.getLabel(freeCtx);
		}
		
		if (label.startsWith("Agenda Week")) {
			label = label.substring("Agenda Week".length()).trim();
			if (label.contains(" ") && label.length() == 7) {				
				label = label.substring(3, 7)+"-W"+label.substring(0, 2);				
			}
		}
		
		if (currentPage.getUrlNumber() > 0) {
			label = label + '-' +currentPage.getUrlNumber();
		}
		String path = URLEncoder.encode(StringHelper.createI18NURL(label), ContentContext.CHARACTER_ENCODING);

		String url = path;
		MenuElement sectionPage = getSectionPage(currentPage);
		if (sectionPage != null) {
			url = URLHelper.mergePath(StringHelper.createI18NURL(sectionPage.getLabel(freeCtx)), url);
		}
		url = '/' + url;
				
		String baseURL = url;
		int i=1;
		while (this.addAndCheckExistURL(currentPage, url)) {
			url = baseURL+'_'+i;
			i++;
		}

		return url;
	}

	@Override
	public String createURL(ContentContext ctx, MenuElement currentPage) throws Exception {		
		if (currentPage.isLikeRoot(ctx)) {
			return "/";
		}		
		return createURLWithoutExt(ctx, currentPage) + '.' + ctx.getFormat();
	}

}
