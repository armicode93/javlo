package org.javlo.component.ecom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.javlo.actions.IAction;
import org.javlo.component.core.IContentVisualComponent;
import org.javlo.component.properties.AbstractPropertiesComponent;
import org.javlo.context.ContentContext;
import org.javlo.ecom.Basket;
import org.javlo.ecom.EcomConfig;
import org.javlo.ecom.Product;
import org.javlo.ecom.Product.ProductBean;
import org.javlo.helper.DebugHelper;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.helper.XHTMLHelper;
import org.javlo.i18n.I18nAccess;
import org.javlo.message.GenericMessage;
import org.javlo.message.MessageRepository;
import org.javlo.navigation.MenuElement;
import org.javlo.service.ContentService;
import org.javlo.service.RequestService;

public class ProductComponent extends AbstractPropertiesComponent implements IAction, IProductContainer {

	private static final long MAX_STOCK = 999999;

	static final List<String> FIELDS_STOCK = Arrays.asList(new String[] { "name", "label", "description", "price", "special_link", "vat", "promo", "currency", "offset", "weight", "production", "basket-page", "html" });

	static final List<String> FIELDS_NOSTOCK = Arrays.asList(new String[] { "name", "label", "description", "price", "special_link", "vat", "promo", "currency", "basket-page", "weight" });

	static final List<String> FIELDS_I18N = Arrays.asList(new String[] { "price", "special_link", "vat", "promo", "currency", "offset", "weight", "production", "basket-page", "html" });

	public static final String TYPE = "product";

	@Override
	public String getHeader() {
		return "Article V 1.0";
	}

	@Override
	public List<String> getFields(ContentContext ctx) throws Exception {
		EcomConfig config = ctx.getGlobalContext().getEcomConfig();
		if (config.isStock()) {
			return FIELDS_STOCK;
		} else {
			return FIELDS_NOSTOCK;
		}
	}

	@Override
	protected boolean getColumnableDefaultValue() {
		return true;
	}

	public String getType() {
		return TYPE;
	}

	public String getName() {
		return getFieldValue("name");
	}

	public String getLabel() {
		return getFieldValue("label");
	}

	public String getDescription() {
		return getFieldValue("description");
	}

	@Override
	public String getPageDescription(ContentContext ctx) {
		return getDescription();
	}

	public String getSpecialLink() {
		return getFieldValue("special_link");
	}

	public double getPrice() {
		return getFieldDoubleValue("price");
	}

	public double getVAT() {
		return getFieldDoubleValue("vat");
	}

	public double getReduction() {
		return getFieldDoubleValue("promo");
	}

	public String getCurrency() {
		return getFieldValue("currency", "EUR");
	}

	public String getBasketPage() {
		return getFieldValue("basket-page", "");
	}

	public long getOffset(ContentContext ctx) {
		if (!ctx.getGlobalContext().getEcomConfig().isStock()) {
			return 1;
		}
		long offset = getFieldLongValue("offset");
		if (offset < 1) {
			offset = 1;
		}
		return offset;
	}

	public long getWeight() {
		return getFieldLongValue("weight");
	}

	public String getHtmlView(ContentContext ctx) throws Exception {
		return XHTMLHelper.replaceJSTLData(ctx, getFieldValue("html_view", ""));
	}

	public String getHtmlAdd(ContentContext ctx) throws Exception {
		return XHTMLHelper.replaceJSTLData(ctx, getFieldValue("html_add", ""));
	}

	public long getProduction() {
		return getFieldLongValue("production");
	}

	public long getRealStock(ContentContext ctx) {
		try {
			if (!ctx.getGlobalContext().getEcomConfig().isStock()) {
				return MAX_STOCK;
			}
			ProductComponent refComp = (ProductComponent) getReferenceComponent(ctx);
			refComp.loadViewData(ctx);
			String value = refComp.getViewData(ctx).getProperty("stock");
			return Long.valueOf(value);
		} catch (Exception e) {
			logger.log(Level.FINE, "invalid real stock, setting to zero...");
			setRealStock(ctx, 0);
			return 0;
		}
	}

	public long getVirtualStock(ContentContext ctx) {
		try {
			if (!ctx.getGlobalContext().getEcomConfig().isStock()) {
				return MAX_STOCK;
			}			
			ProductComponent refComp = (ProductComponent) getReferenceComponent(ctx);
			refComp.loadViewData(ctx);
			if (refComp != null) {
				String value = refComp.getViewData(ctx).getProperty("virtual");
				return Long.valueOf(value);
			} else {
				logger.severe("refComp not found.");
				return -1;
			}

		} catch (Exception e) {
			logger.log(Level.FINE, "invalid virtual stock, setting to zero...");
			setVirtualStock(ctx, 0);
			return 0;
		}
	}

	public void setRealStock(ContentContext ctx, long realStock) {
		try {
			ProductComponent refComp = (ProductComponent) getReferenceComponent(ctx);
			refComp.getViewData(ctx).setProperty("stock", String.valueOf(realStock));
			refComp.storeViewData(ctx);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setVirtualStock(ContentContext ctx, long virtualStock) {
		ProductComponent refComp;
		try {
			refComp = (ProductComponent) getReferenceComponent(ctx);
			refComp.getViewData(ctx).put("virtual", String.valueOf(virtualStock));
			refComp.storeViewData(ctx);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected String getEditXHTMLCode(ContentContext ctx) throws Exception {
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);

		List<String> fields = getFields(ctx);
		out.println("<div class=\"edit\" style=\"padding: 3px;\"><div class=\"row\">");

		for (String field : fields) {
			boolean i18n = FIELDS_I18N.contains(field);
			ProductComponent comp = this;
			if (i18n) {
				comp = (ProductComponent) getReferenceComponent(ctx);
				if (comp == null) {
					return "reference component not found, check the page structure in : " + ctx.getGlobalContext().getDefaultLanguage();
				}
			}
			renderField(ctx, out, field, getRowSize(field), comp.getFieldValue(field), i18n);
		}

		if (ctx.getGlobalContext().getEcomConfig().isStock()) {
			renderField(ctx, out, "stock", 1, getRealStock(ctx), true);
			renderField(ctx, out, "virtual", 1, getVirtualStock(ctx), true);
		}

		out.println("</div></div>");

		out.flush();
		out.close();
		return writer.toString();
	}

	private void renderField(ContentContext ctx, PrintWriter out, String field, int rowSize, Object value, boolean i18n) throws Exception {
		I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());

		String fieldId = createKeyWithField(field);

		out.println("<div class=\"col-md-4 col-lg-3\"><div class=\"form-group\">");
		out.println("<label for=\"" + fieldId + "\">" + i18nAccess.getText("field." + field) + "</label>");

		String readonly = "";
		if (i18n && !ctx.getRequestContentLanguage().equals(ctx.getGlobalContext().getDefaultLanguage())) {
			readonly = " readonly=\"readonly\"";
		}

		out.print("<textarea class=\"form-control\" rows=\"" + rowSize + "\" id=\"" + fieldId + "\" name=\"" + fieldId + "\"" + readonly + ">");
		out.print(String.valueOf(value));
		out.println("</textarea>");
		out.println("</div></div>");
	}

	@Override
	public String performEdit(ContentContext ctx) throws Exception {
		String msg = super.performEdit(ctx);

		RequestService requestService = RequestService.getInstance(ctx.getRequest());

		String stockValue = requestService.getParameter(createKeyWithField("stock"), null);
		try {
			long stock = Long.valueOf(stockValue);
			if (stock != getRealStock(ctx)) {
				setRealStock(ctx, stock);
			}
		} catch (Exception e) {
		}
		String virtualValue = requestService.getParameter(createKeyWithField("virtual"), null);
		try {
			long virtual = Long.valueOf(virtualValue);
			if (virtual != getVirtualStock(ctx)) {
				setVirtualStock(ctx, virtual);
			}
		} catch (Exception e) {
		}

		return msg;
	}

	private static final String getCurrencyHtml(String cur) {
		if (cur.equalsIgnoreCase("eur")) {
			return "&euro;";
		} else {
			return cur;
		}
	}

	public static void main(String[] args) {
		double test = 34.2;

		System.out.println(">> " + StringHelper.renderDouble(test, 2));
	}

	@Override
	public void prepareView(ContentContext ctx) throws Exception {
		super.prepareView(ctx);

		ctx.getRequest().setAttribute("productName", getName());

		String action;
		if (getBasketPage().trim().length() > 0) {
			ContentService content = ContentService.getInstance(ctx.getRequest());
			MenuElement page = content.getNavigation(ctx).searchChildFromName(getBasketPage());
			if (page != null) {
				action = URLHelper.createURL(ctx, page);
			} else {
				action = URLHelper.createURL(ctx);
			}
		} else {
			action = URLHelper.createURL(ctx);
		}

		ProductComponent refComp = (ProductComponent) getReferenceComponent(ctx);
		if (refComp != null) {
			ctx.getRequest().setAttribute("action", action);
			ctx.getRequest().setAttribute("price", refComp.getPrice());
			ctx.getRequest().setAttribute("priceDisplay", StringHelper.renderDouble(refComp.getPrice(), 2));
			ctx.getRequest().setAttribute("currency", refComp.getCurrency());
			ctx.getRequest().setAttribute("currencyDisplay", getCurrencyHtml(refComp.getCurrency()));
			if (!StringHelper.isEmpty(getDescription())) {
				ctx.getRequest().setAttribute("description", XHTMLHelper.textToXHTML(getDescription()));
			}
			ctx.getRequest().setAttribute("virtualStock", getVirtualStock(ctx));
			ctx.getRequest().setAttribute("offset", refComp.getOffset(ctx));

			if (!StringHelper.isEmpty(refComp.getSpecialLink())) {
				String link = refComp.getSpecialLink();
				link = XHTMLHelper.replaceJSTLData(ctx, link);
				link = XHTMLHelper.replaceLinks(ctx, link);
				ctx.getRequest().setAttribute("specialLink", link);
			}
		}
	}

	@Override
	public String getViewXHTMLCode(ContentContext ctx) throws Exception {
		if (getOffset(ctx) > 0) {
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			PrintStream out = new PrintStream(outStream);

			String action;
			if (getBasketPage().trim().length() > 0) {
				ContentService content = ContentService.getInstance(ctx.getRequest());
				MenuElement page = content.getNavigation(ctx).searchChildFromName(getBasketPage());
				if (page != null) {
					action = URLHelper.createURL(ctx, page);
				} else {
					action = URLHelper.createURL(ctx);
				}
			} else {
				action = URLHelper.createURL(ctx);
			}

			out.println("<form role=\"form\" class=\"mb-3 add-basket\" id=\"product-" + getName() + "_" + getId() + "\" method=\"post\" action=\"" + action + "\">");
			out.println("<input type=\"hidden\" name=\"webaction\" value=\"products.buy\" />");
			out.println("<input type=\"hidden\" name=\"cid\" value=\"" + getId() + "\" />");
			I18nAccess i18nAccess = I18nAccess.getInstance(ctx);

			out.println("<div class=\"list-group\">");
			if (!StringHelper.isEmpty(getName())) {
				out.println("<div class=\"line list-group-item name\">");
				out.println("<h2>" + getName() + "</h2>");
				out.println("</div>");
			}

			double price = getPrice();
			out.println("<div class=\"line list-group-item price d-flex justify-content-between\">");
			if (price > 0) {
				out.println("<span class=\"label\">" + i18nAccess.getViewText("ecom.price") + "</span> <span class=\"price\">" + StringHelper.renderDouble(price, 2) + "&nbsp;" + getCurrencyHtml(getCurrency()) + "</span>");
			} else {
				out.println("<span class=\"label\">" + i18nAccess.getViewText("ecom.gift") + "&nbsp; (" + getCurrencyHtml(getCurrency()) + ")</span> <span class=\"price\"><input class=\"form-control digit\" name=\"price\" type=\"number\" min=\"2\" value=\"\" /></span>");
			}
			out.println("</div>");

			if (!StringHelper.isEmpty(getDescription())) {
				out.println("<div class=\"line list-group-item description\">");
				out.println("<p>" + XHTMLHelper.textToXHTML(getDescription()) + "</p>");
				out.println("</div>");
			}

			// out.println("<div class=\"line list-group-item stock d-flex
			// justify-content-between\">");
			// out.println("<span class=\"label\">" + i18nAccess.getViewText("ecom.stock") +
			// "</span> <span class=\"stock\">"+getRealStock(ctx)+"</span>");
			// out.println("</div>");

			out.println("<div class=\"line list-group-item stock d-flex justify-content-between form-inline\">");
			if (getVirtualStock(ctx) > getOffset(ctx)) {

				String Qid = "product-" + StringHelper.getRandomId();
				if (price > 0) {
					out.println("<label class=\"quantity-label\" for=\"" + Qid + "\"><span>" + i18nAccess.getViewText("ecom.quantity") + "</span></label>");
					out.println("<input class=\"form-control digit\" id=\"" + Qid + "\" type=\"number\" min=\"1\" name=\"quantity\" value=\"" + getOffset(ctx) + "\" maxlength=\"3\"/>");
				} else {
					out.println("<div><input type=\"hidden\" name=\"quantity\" value=\"1\" /></div>");
				}
				out.println(
						"<span class=\"buy\"><button class=\"btn btn-default btn-primary buy\" type=\"submit\" name=\"buy\"><svg style=\"vertical-align: baseline;\" xmlns=\"http://www.w3.org/2000/svg\" width=\"14\" height=\"14\" fill=\"currentColor\" class=\"bi bi-basket\" viewBox=\"0 0 16 16\">\r\n" + "  <path d=\"M5.757 1.071a.5.5 0 0 1 .172.686L3.383 6h9.234L10.07 1.757a.5.5 0 1 1 .858-.514L13.783 6H15a1 1 0 0 1 1 1v1a1 1 0 0 1-1 1v4.5a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 1 13.5V9a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h1.217L5.07 1.243a.5.5 0 0 1 .686-.172zM2 9v4.5A1.5 1.5 0 0 0 3.5 15h9a1.5 1.5 0 0 0 1.5-1.5V9H2zM1 7v1h14V7H1zm3 3a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-1 0v-3A.5.5 0 0 1 4 10zm2 0a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-1 0v-3A.5.5 0 0 1 6 10zm2 0a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-1 0v-3A.5.5 0 0 1 8 10zm2 0a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-1 0v-3a.5.5 0 0 1 .5-.5zm2 0a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-1 0v-3a.5.5 0 0 1 .5-.5z\"/>\r\n" + "</svg> " + i18nAccess.getViewText("ecom.buy") + "</button></span>");

				if (!StringHelper.isEmpty(getSpecialLink())) {
					String link = getSpecialLink();
					link = XHTMLHelper.replaceJSTLData(ctx, link);
					link = XHTMLHelper.replaceLinks(ctx, link);
					out.println("<span class=\"special-link\">" + link + "</span>");
				}

				String dinfo = i18nAccess.getViewText("ecom.delivery-info", "");
				if (!StringHelper.isEmpty(dinfo)) {
					out.println("<div class=\"delivery-info\">" + dinfo + "</div>");
				}

				out.println("</div>");
			} else {
				out.println("<span class=\"soldout\">" + i18nAccess.getViewText("ecom.soldout") + "</span>");
			}
			out.println("</div>");

			out.println("</form>");

			if (StringHelper.isEmpty(ctx.getRequest().getParameter("cid"))) {
				out.println(getHtmlView(ctx));
			} else {
				out.println(getHtmlAdd(ctx));
			}

			out.close();
			return new String(outStream.toByteArray());
		} else {
			return "<div class=\"alert alert-danger\">" + getType() + " : offset not found.</div>";
		}
	}

	@Override
	public String getHexColor() {
		return ECOM_COLOR;
	}

	@Override
	public String getActionGroupName() {
		return "products";
	}

	@Override
	public int getLabelLevel(ContentContext ctx) {
		return 1;
	}

	@Override
	public String getTextTitle(ContentContext ctx) {
		return getName();
	}

	@Override
	public String getTextLabel(ContentContext ctx) {
		return getName();
	}

	public static String performBuy(RequestService rs, ContentContext ctx, MessageRepository messageRepository, I18nAccess i18nAccess) throws Exception {

		ContentService content = ContentService.getInstance(ctx.getRequest());
		MenuElement currentPage = ctx.getCurrentPage();

		/* information from product */
		String cid = rs.getParameter("cid", null);
		if (cid != null) {
			IContentVisualComponent comp = content.getComponent(ctx, cid);
			if ((comp != null) && (comp instanceof ProductComponent)) {
				ProductComponent pComp = (ProductComponent) comp;
				Product product = new Product(pComp, (ProductComponent) pComp.getReferenceComponent(ctx));

				if (StringHelper.isDigit(rs.getParameter("price"))) {
					product.setPrice(Double.parseDouble(rs.getParameter("price")));
				}

				/* information from page */
				product.setUrl(URLHelper.createURL(ctx.getContextForAbsoluteURL(), currentPage));
				product.setPreviewUrl(URLHelper.createURL(ctx.getContextWithOtherRenderMode(ContentContext.PREVIEW_MODE).getContextForAbsoluteURL(), currentPage));
				product.setShortDescription(currentPage.getTitle(ctx));
				product.setLongDescription(currentPage.getDescriptionAsText(ctx));
				if (currentPage.getImage(ctx) != null) {
					product.setImage(ctx, currentPage.getImage(ctx));
				}

				String quantity = rs.getParameter("quantity", null);
				if (quantity != null) {
					int quantityValue = Integer.parseInt(quantity);

					quantityValue = quantityValue - (quantityValue % (int) pComp.getOffset(ctx));
					product.setQuantity(quantityValue);

					if (quantityValue > pComp.getVirtualStock(ctx)) {
						return i18nAccess.getViewText("ecom.error.no-stock");
					}

					Basket basket = Basket.getInstance(ctx);
					if (basket.isLock()) {
						return i18nAccess.getText("ecom.basket-lock");
					}
					basket.addProduct(product);

					String msg = i18nAccess.getViewText("ecom.product.add", new String[][] { { "product", pComp.getName() } });
					messageRepository.setGlobalMessage(new GenericMessage(msg, GenericMessage.INFO));

					String redirectPage = comp.getConfig(ctx).getProperty("buy.target-page", null);
					if (redirectPage != null) {
						MenuElement targetPage = ctx.getCurrentPage().getRoot().searchChildFromName(redirectPage);
						if (targetPage == null) {
							logger.severe("page not found : " + targetPage);
						} else {
							ctx.setPath(targetPage.getPath());
							comp.prepareView(ctx);
						}
					}
				}
			}
		}

		return null;
	}

	@Override
	public ProductBean getProductBean(ContentContext ctx) {
		try {
			return new Product(this, (ProductComponent) getReferenceComponent(ctx)).getBean(ctx);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
