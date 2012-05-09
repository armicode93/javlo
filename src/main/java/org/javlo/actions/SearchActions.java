/** 
* Created on Aug 13, 2003
*/
package org.javlo.actions;

import java.text.ParseException;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.javlo.context.ContentContext;
import org.javlo.helper.StringHelper;
import org.javlo.search.SearchResult;
import org.javlo.service.RequestService;
import org.javlo.template.TemplateSearchContext;


/**
 * @author pvandermaesen
 * list of actions for search in cms.
 */
public class SearchActions implements IAction {
	
	/**
	 * create a static logger.
	 */
	protected static Logger logger = Logger.getLogger(SearchActions.class.getName());

	public static String performSearch(HttpServletRequest request, HttpServletResponse response) {
		String msg = null;
		
		try {

			String searchStr = request.getParameter("keywords");
			String groupId = request.getParameter("search-group");
			String sort = request.getParameter("sort");
			
			logger.info("search action : "+searchStr);
			
			if (searchStr != null) {
				if (searchStr.length() > 0) {
					ContentContext ctx = ContentContext.getContentContext(request, response);
					
					ctx.setSpecialContentRenderer("/jsp/search/search_result.jsp");
					
					if (ctx.getCurrentTemplate() != null && ctx.getCurrentTemplate().getSearchRenderer(ctx) != null) {
						ctx.setSpecialContentRenderer(ctx.getCurrentTemplate().getSearchRenderer(ctx));
					}

					SearchResult search = SearchResult.getInstance(request.getSession());
					search.search(ctx, groupId, searchStr, sort);
					
					ctx.getRequest().getSession().setAttribute("searchList", search.getSearchResult());
					
				} else {
					msg = "error search strign not defined";
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			msg = e.getMessage();
		}

		return msg;
	}
	
	public static String performTemplate(HttpServletRequest request, HttpServletResponse response) throws Exception {
		RequestService requestService = RequestService.getInstance(request);
		
		TemplateSearchContext tempCtx = TemplateSearchContext.getInstance(request.getSession());
		tempCtx.setAuthors(requestService.getParameter("authors", ""));
		tempCtx.setSource(requestService.getParameter("source", ""));
		tempCtx.setDominantColor(requestService.getParameter("dominant_color", ""));
		try {
			String dateStr = requestService.getParameter("date", "");
			if (dateStr.trim().length() == 0) {
				tempCtx.setDate(null);
			} else {
				Date date = StringHelper.parseDate(dateStr);
				tempCtx.setDate(date);
			}
		} catch (ParseException e) {
			tempCtx.setDate(null);
		}
		try {
			tempCtx.setDepth(Integer.parseInt(requestService.getParameter("depth", "")));
		} catch (NumberFormatException e) {
		}
		return null;
	}

	public String getActionGroupName() {
		return "search";
	}

}
