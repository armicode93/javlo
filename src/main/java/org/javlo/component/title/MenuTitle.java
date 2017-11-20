/*
 * Created on 19-sept.-2003
 */
package org.javlo.component.title;

import org.javlo.component.core.AbstractVisualComponent;
import org.javlo.context.ContentContext;


/**
 * @author pvandermaesen
 */
public class MenuTitle extends AbstractVisualComponent {
	
	public static final String TYPE = "menu-title";

	@Override
	public int getSearchLevel() {
		return SEARCH_LEVEL_HIGH;
	}

	/**
	 * @see org.javlo.itf.IContentVisualComponent#getXHTMLCode()
	 */
	@Override
	public String getViewXHTMLCode(ContentContext ctx) throws Exception {
		return "";
	}

	public String getType() {
		return TYPE;
	}
	
	@Override
	public String getTextLabel(ContentContext ctx) {
		return getValue();
	}
	
	@Override
	public String getHexColor() {
		return META_COLOR;
	}
	
	@Override
	public String getEmptyXHTMLCode(ContentContext ctx) throws Exception {
		String emptyCode = super.getEmptyXHTMLCode(ctx);		
		if (emptyCode != null && emptyCode.length()>0) {
			return emptyCode.substring(0, emptyCode.length()-1)+" : "+getValue()+']';
		} else {
			return "";
		}
	}
	
	@Override
	public boolean isContentCachable(ContentContext ctx) {
		return true;
	}
	
	@Override
	public int getComplexityLevel(ContentContext ctx) {
		return getConfig(ctx).getComplexity(COMPLEXITY_STANDARD);
	}
	
	@Override
	public String getFontAwesome() {	
		return "level-up";
	}

}
