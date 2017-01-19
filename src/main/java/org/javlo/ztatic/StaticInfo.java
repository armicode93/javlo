package org.javlo.ztatic;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.naming.ConfigurationException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.javlo.cache.ICache;
import org.javlo.config.StaticConfig;
import org.javlo.context.ContentContext;
import org.javlo.context.GlobalContext;
import org.javlo.helper.ExifHelper;
import org.javlo.helper.ResourceHelper;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.navigation.MenuElement;
import org.javlo.service.ContentService;
import org.javlo.service.NavigationService;
import org.javlo.service.PersistenceService;
import org.javlo.service.exception.ServiceException;
import org.javlo.service.location.LocationService;
import org.javlo.user.User;
import org.javlo.ztatic.InitInterest.Point;
import org.owasp.encoder.Encode;

public class StaticInfo {

	public static final String _STATIC_INFO_DIR = null;

	public static final int DEFAULT_FOCUS_X = 500;

	public static final int DEFAULT_FOCUS_Y = 500;

	/**
	 * create a static logger.
	 */
	protected static Logger logger = Logger.getLogger(StaticInfo.class.getName());

	public static final class Position {
		private double longitude = 0;
		private double latitude = 0;

		public Position(double longitude, double latiture) {
			super();
			this.longitude = longitude;
			this.latitude = latiture;
		}

		public double getLongitude() {
			return longitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		public double getLatitude() {
			return latitude;
		}

		public void setLatitude(double latiture) {
			this.latitude = latiture;
		}

		@Override
		public String toString() {
			return getLatitude() + "," + getLongitude();
		}

	}

	public static final class StaticInfoBean {
		private final ContentContext ctx;
		private final StaticInfo staticInfo;
		private final StaticInfoBean folder;
		private String key = null;

		public StaticInfoBean(ContentContext ctx, StaticInfo staticInfo) throws Exception {
			this.ctx = ctx;
			this.staticInfo = staticInfo;
			if (staticInfo.getFile().isFile()) {
				this.folder = new StaticInfoBean(ctx, StaticInfo.getInstance(ctx, staticInfo.getFile().getParentFile()));
			} else {
				this.folder = null;
			}
		}

		public String getId() {
			return staticInfo.getId(ctx);
		}

		public String getTitle() {
			return staticInfo.getManualTitle(ctx);
		}

		public String getHtmlTitle() {
			return Encode.forHtmlAttribute(staticInfo.getManualTitle(ctx));
		}

		public String getDescription() {
			return staticInfo.getManualDescription(ctx);
		}					

		public String getHtmlDescription() {
			return Encode.forHtmlAttribute(staticInfo.getManualDescription(ctx));
		}

		public String getCopyright() {
			return staticInfo.getCopyright(ctx);
		}

		public String getHtmlCopyright() {
			return Encode.forHtmlAttribute(staticInfo.getCopyright(ctx));
		}

		public String getLocation() {
			return staticInfo.getManualLocation(ctx);
		}

		public String getHtmlLocation() {
			return Encode.forHtmlAttribute(staticInfo.getManualLocation(ctx));
		}

		public String getAccessToken() {
			return staticInfo.getAccessToken(ctx);
		}

		public String getFullTitle() {
			try {
				return staticInfo.getFullTitle(ctx);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		public String getHtmlFullTitle() {
			try {
				return Encode.forHtmlAttribute(staticInfo.getFullTitle(ctx));
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		public String getSortableDate() {
			return StringHelper.renderSortableTime(staticInfo.getDate(ctx));
		}

		public String getShortDate() {
			try {
				return StringHelper.renderShortDate(ctx, staticInfo.getDate(ctx));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public String getFullDate() {
			try {
				return StringHelper.renderFullDate(ctx, staticInfo.getDate(ctx));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public StaticInfo getStaticInfo() {
			return staticInfo;
		}

		/**
		 * get manual key (jstl)
		 * 
		 * @return
		 */
		public String getKey() {
			return key;
		}

		/**
		 * set manual key (jstl)
		 * 
		 * @param key
		 */
		public void setKey(String key) {
			this.key = key;
		}

		public StaticInfoBean getFolder() {
			return folder;
		}

		public Position getPosition() {
			return staticInfo.getPosition(ctx);
		}

		public String getName() {
			return staticInfo.getFile().getName();
		}

		public String getURL() {
			return URLHelper.createResourceURL(ctx, staticInfo.getFile());
		}

		public int getFocusZoneX() {
			return staticInfo.getFocusZoneX(ctx);
		}

		public int getFocusZoneY() {
			return staticInfo.getFocusZoneY(ctx);
		}
		public boolean isEmptyInfo() {
			if (!StringHelper.isEmpty(getTitle())) {
				return false;
			}
			if (!StringHelper.isEmpty(getDescription())) {
				return false;
			}
			if (!StringHelper.isEmpty(getCopyright())) {
				return false;
			}
			if (!StringHelper.isEmpty(getLocation())) {
				return false;
			}
			return true;
		}
		private String getAllInfo(boolean html) {
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			PrintStream out = new PrintStream(outStream);
			String sep = "";
			String baseSep = "<span class=\"sep\">-</span>";
			if (!html) {
				baseSep = " - ";
			}
			String title = getTitle();				
			if (!StringHelper.isEmpty(title)) {
				if (html) {
					out.print("<span class=\"title\">"+Encode.forHtml(title)+"</span>");
				} else {
					out.print(title);
				}
				sep = baseSep;
			}
			Date date = staticInfo.getManualDate(ctx);				
			if (date != null) {
				if (html) {
					out.print(sep+"<span class=\"date\">"+getShortDate()+"</span>");
				} else {
					out.print(sep+getShortDate());
				}
				sep = baseSep;
			}
			String description = getDescription();				
			if (!StringHelper.isEmpty(description)) {
				if (html) {
					out.print(sep+"<span class=\"description\">"+Encode.forHtml(description)+"</span>");
				} else {
					out.print(sep+description);
				}
				sep = baseSep;
			}
			String location = getLocation();				
			if (!StringHelper.isEmpty(location)) {
				if (html) {
					out.print(sep+"<span class=\"location\">"+Encode.forHtml(location)+"</span>");
				} else {
					out.print(sep+location);
				}
				sep = baseSep;
			}
			String copyright = getCopyright();				
			if (!StringHelper.isEmpty(copyright)) {
				if (html) {
					out.print(sep+"<span class=\"copyright\">"+Encode.forHtml(copyright)+"</span>");
				} else {
					out.print(sep+copyright);
				}
				sep = baseSep;
			}
			out.println();
			out.close();
			String outStr = new String(outStream.toByteArray());
			if (!html) {
				outStr = Encode.forHtmlAttribute(outStr);
			}
			return outStr;
		}
		public String getAllInfoHtml() {
			return getAllInfo(true);
		}
		public String getAllInfoText() {
			return getAllInfo(false);
		}
	}
	
	public ContentContext getContextWithContent(ContentContext ctx) {
		String content = (getTitle(ctx)+getDescription(ctx)+getCopyright(ctx)).trim();
		if (content.length()>0) {
			return ctx;
		} else {
			ContentContext lgCtx = new ContentContext(ctx);
			for (String lg : ctx.getGlobalContext().getDefaultLanguages()) {
				lgCtx.setAllLanguage(lg);
				content = (getTitle(lgCtx)+getDescription(lgCtx)+getCopyright(lgCtx)).trim();
				if (content.length() > 0) {
					return lgCtx;
				}
			}			
		}
		return ctx;
	}

	public static class StaticSort implements Comparator<String> {

		ContentContext ctx;
		String baseFolder;
		boolean ascending = true;

		public StaticSort(ContentContext inCtx, String inBaseFolder, boolean inAscending) {
			ctx = inCtx;
			baseFolder = StringHelper.neverEmpty(inBaseFolder, "");
			ascending = inAscending;
		}

		@Override
		public int compare(String uri1, String uri2) {
			try {
				String fullURI1 = URLHelper.mergePath(baseFolder, uri1);
				String fullURI2 = URLHelper.mergePath(baseFolder, uri2);

				StaticInfo uri1Info = StaticInfo.getInstance(ctx, fullURI1);
				StaticInfo uri2Info = StaticInfo.getInstance(ctx, fullURI2);

				int changeOrder = 1;
				if (!ascending) {
					changeOrder = -1;
				}

				if (uri1Info.getDate(ctx) == null && uri2Info.getDate(ctx) == null) {
					return uri1.compareTo(uri2) * changeOrder;
				} else if (uri1Info.getDate(ctx) == null) {
					return -1 * changeOrder;
				} else if (uri2Info.getDate(ctx) == null) {
					return 1 * changeOrder;
				} else {
					return uri1Info.getDate(ctx).compareTo(uri2Info.getDate(ctx)) * changeOrder;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

	}

	public static class StaticFileSort implements Comparator<File> {

		ContentContext ctx;
		boolean ascending = true;

		public StaticFileSort(ContentContext inCtx, boolean inAscending) {
			ctx = inCtx;
			ascending = inAscending;
		}

		@Override
		public int compare(File file1, File file2) {
			try {
				StaticInfo uri1Info = StaticInfo.getInstance(ctx, file1);
				StaticInfo uri2Info = StaticInfo.getInstance(ctx, file2);

				int changeOrder = 1;
				if (!ascending) {
					changeOrder = -1;
				}

				if (uri1Info.getDate(ctx) == null && uri2Info.getDate(ctx) == null) {
					return file1.compareTo(file2) * changeOrder;
				} else if (uri1Info.getDate(ctx) == null) {
					return -1 * changeOrder;
				} else if (uri2Info.getDate(ctx) == null) {
					return 1 * changeOrder;
				} else {
					if (uri1Info.getDate(ctx).equals(uri2Info.getDate(ctx))) {
						return 0;
					} else {
						Calendar cal1 = Calendar.getInstance();
						cal1.setTime(uri1Info.getDate(ctx));
						Calendar cal2 = Calendar.getInstance();
						cal2.setTime(uri2Info.getDate(ctx));
						if (cal1.compareTo(cal2) > 0) {
							return -1;
						} else {
							return 1;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

	}

	public static class StaticFileSortByAccess implements Comparator<File> {

		ContentContext ctx;
		boolean ascending = true;

		private final Map<File, Integer> fileAccess = new HashMap<File, Integer>();

		public StaticFileSortByAccess(ContentContext inCtx, boolean inAscending) {
			ctx = inCtx;
			ascending = inAscending;
		}

		@Override
		public int compare(File file1, File file2) {
			try {
				int changeOrder = 1;
				if (!ascending) {
					changeOrder = -1;
				}

				Integer access1 = fileAccess.get(file1);
				if (access1 == null) {
					StaticInfo uri1Info = StaticInfo.getInstance(ctx, file1);
					access1 = uri1Info.getAccessFromSomeDays(ctx);
					fileAccess.put(file1, access1);
				}

				Integer access2 = fileAccess.get(file2);
				if (access2 == null) {
					StaticInfo uri2Info = StaticInfo.getInstance(ctx, file2);
					access2 = uri2Info.getAccessFromSomeDays(ctx);
					fileAccess.put(file2, access2);
				}

				if (access1.intValue() == access2.intValue()) {
					return changeOrder;
				} else {
					return (access1 - access2) * changeOrder;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

	}

	public static class StaticFileSortByPopularity implements Comparator<File> {

		ContentContext ctx;
		boolean ascending = true;

		public StaticFileSortByPopularity(ContentContext inCtx, boolean inAscending) {
			ctx = inCtx;
			ascending = inAscending;
		}

		@Override
		public int compare(File file1, File file2) {
			try {
				StaticInfo uri1Info = StaticInfo.getInstance(ctx, file1);
				StaticInfo uri2Info = StaticInfo.getInstance(ctx, file2);

				int changeOrder = 1;
				if (!ascending) {
					changeOrder = -1;
				}

				if (uri1Info.getAccessFromSomeDays(ctx) == uri2Info.getAccessFromSomeDays(ctx)) {
					if (uri1Info.getDate(ctx) == null && uri2Info.getDate(ctx) == null) {
						return file1.compareTo(file2) * changeOrder;
					} else if (uri1Info.getDate(ctx) == null) {
						return -1 * changeOrder;
					} else if (uri2Info.getDate(ctx) == null) {
						return 1 * changeOrder;
					} else {
						return uri1Info.getDate(ctx).compareTo(uri2Info.getDate(ctx)) * changeOrder;
					}
				} else {
					return (uri1Info.getAccessFromSomeDays(ctx) - uri2Info.getAccessFromSomeDays(ctx)) * changeOrder;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

	}

	public static class StaticInfoSortByDate implements Comparator<StaticInfo> {

		ContentContext ctx;
		boolean ascending = true;

		public StaticInfoSortByDate(ContentContext inCtx, boolean inAscending) {
			ctx = inCtx;
			ascending = inAscending;
		}

		@Override
		public int compare(StaticInfo uri1Info, StaticInfo uri2Info) {
			try {
				int changeOrder = 1;
				if (!ascending) {
					changeOrder = -1;
				}

				if (uri1Info.getDate(ctx) == null && uri2Info.getDate(ctx) == null) {
					return 1;
				} else if (uri1Info.getDate(ctx) == null) {
					return -1 * changeOrder;
				} else if (uri2Info.getDate(ctx) == null) {
					return 1 * changeOrder;
				} else {
					if (uri1Info.getDate(ctx).equals(uri2Info.getDate(ctx))) {
						return 1;
					} else {
						return uri1Info.getDate(ctx).compareTo(uri2Info.getDate(ctx)) * changeOrder;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

	}

	public static class StaticInfoSortByCreationDate implements Comparator<StaticInfo> {
		ContentContext ctx;
		boolean ascending = true;

		public StaticInfoSortByCreationDate(ContentContext inCtx, boolean inAscending) {
			ctx = inCtx;
			ascending = inAscending;
		}

		@Override
		public int compare(StaticInfo uri1Info, StaticInfo uri2Info) {
			try {
				int changeOrder = 1;
				if (!ascending) {
					changeOrder = -1;
				}
				return uri1Info.getCreationDate(ctx).compareTo(uri2Info.getCreationDate(ctx)) * changeOrder;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}
	}

	private static final String KEY = "staticinfo-";

	public static final int ACCES_DAYS = 7;

	private String staticURL = null;

	private MenuElement linkedPage = null;

	private Date linkedDate;

	private Date fileDate = null;

	private String linkedTitle;

	private String linkedDescription;

	private String linkedLocation;

	private List<String> tags = null;

	private List<String> readRoles = null;

	private long size = -1;

	private File file;

	private Long crc32 = null;

	private Date date;

	private String id = null;

	private boolean staticFolder = true;

	/**
	 * false if date come from last modified of the file.
	 */
	private boolean dateFromData = true;

	private int accessFromSomeDays = -1;

	// private Metadata imageMetadata = null;

	/**
	 * instance of static info sur shared file
	 * 
	 * @param ctx
	 * @param inStaticURL
	 * @return
	 * @throws ConfigurationException
	 * @throws IOException
	 */
	public static StaticInfo getShareInstance(ContentContext ctx, String inStaticURL) throws Exception {
		return getInstance(ctx, inStaticURL);
	}

	private String getKey(String key) {
		return getKey(staticURL, key);
	}

	private String getKey(String inStaticURL, String key) {
		return KEY + inStaticURL + '-' + key;
	}

	public static StaticInfo getInstance(ContentContext ctx, File file) throws Exception {
		StaticConfig staticConfig = StaticConfig.getInstance(ctx.getRequest().getSession());
		GlobalContext globalContext = ctx.getGlobalContext();
		String fullURL = URLHelper.cleanPath(file.getPath(), false);
		boolean staticFolder = true;
		String fullStaticFolder = URLHelper.mergePath(globalContext.getDataFolder(), staticConfig.getStaticFolder());
		if (ResourceHelper.isTemplateFile(globalContext, file)) {
			fullStaticFolder = staticConfig.getTemplateFolder();
		} else if (!file.getAbsolutePath().contains('/' + staticConfig.getStaticFolder() + '/')) { // before
																									// /static
			fullStaticFolder = globalContext.getDataFolder();
			// staticFolder = false;
		}
		fullStaticFolder = URLHelper.cleanPath(fullStaticFolder, false);
		String relURL = "/";
		if (fullURL.length() > fullStaticFolder.length()) {
			relURL = StringUtils.replace(fullURL, fullStaticFolder, "");
		}
		StaticInfo staticInfo = getInstance(ctx, relURL);
		staticInfo.setStaticFolder(staticFolder);
		return staticInfo;
	}

	public static StaticInfo getInstance(ContentContext ctx, String inStaticURL) throws Exception {
		inStaticURL = inStaticURL.replace('\\', '/').replaceAll("//", "/").trim();
		if (!inStaticURL.startsWith("/")) {
			inStaticURL = '/' + inStaticURL;
		}
		if (inStaticURL.startsWith("/static")) {
			inStaticURL = inStaticURL.replaceFirst("/static", "");
		}

		HttpServletRequest request = ctx.getRequest();

		StaticInfo outStaticInfo = (StaticInfo) request.getAttribute(inStaticURL);

		if (outStaticInfo == null) {
			StaticInfo staticInfo = new StaticInfo();
			// init creation data
			staticInfo.getCreationDate(ctx);
			staticInfo.staticURL = inStaticURL;

			StaticConfig staticConfig = StaticConfig.getInstance(ctx.getRequest().getSession());
			if (inStaticURL.startsWith(staticConfig.getStaticFolder())) {
				inStaticURL = inStaticURL.substring(staticConfig.getStaticFolder().length());
			}
			GlobalContext globalContext = GlobalContext.getInstance(request);
			String realPath = URLHelper.mergePath(globalContext.getDataFolder(), staticConfig.getStaticFolder());
			realPath = URLHelper.mergePath(realPath, inStaticURL);

			File file = new File(realPath);
			staticInfo.setFile(file);
			staticInfo.size = file.length();

			outStaticInfo = staticInfo;
			request.setAttribute(inStaticURL, outStaticInfo);
		}

		return outStaticInfo;
	}

	public String getFullDescription(ContentContext ctx) {
		return getFullDescription(ctx, true);
	}

	public String getFullDescription(ContentContext ctx, boolean showTitle) {

		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);

		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		Iterator<String> defaultLg = globalContext.getDefaultLanguages().iterator();

		ContentContext pertientContext = new ContentContext(ctx);

		while (!isPertinent(pertientContext) && defaultLg.hasNext()) {
			pertientContext.setRequestContentLanguage(defaultLg.next());
		}

		if (!isPertinent(pertientContext)) {
			pertientContext = ctx;
		}

		String title = getTitle(pertientContext);
		if (title != null && title.trim().length() > 0) {
			if ((title.trim().charAt(title.trim().length() - 1) == '.')) {
				title = title.substring(0, title.length() - 1);
			}
			out.print(title);
		}

		/*
		 * if (getDescription() != null && getDescription().trim().length() > 0)
		 * { String description = getDescription().trim(); if
		 * (description.charAt(description.length() - 1) == '.') { description =
		 * description.substring(0, description.length() - 1); } if (getTitle()
		 * != null && getTitle().trim().length() > 0) { out.print(". "); }
		 * out.print(description); }
		 */

		String sufix = "";

		if (getLocation(pertientContext) != null && getLocation(pertientContext).trim().length() > 0) {
			sufix = " (" + getLocation(pertientContext) + " ";
		}

		if (getDate(ctx) != null && !isEmptyDate(ctx)) {
			if (getLocation(pertientContext) == null || getLocation(pertientContext).trim().length() == 0) {
				sufix = sufix + " (";
			}
			sufix = sufix + StringHelper.renderDate(getDate(ctx));
		}

		if (sufix.length() > 0) {
			sufix = sufix + ")";
		}

		out.print(sufix);

		if ((getDescription(pertientContext) != null && getDescription(pertientContext).trim().length() > 0) || (getTitle(pertientContext) != null && getTitle(pertientContext).trim().length() > 0)) {
			out.print('.');
		}

		out.close();
		return writer.toString();
	}

	public String getManualDescription(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		return content.getAttribute(ctx, getKey("description-" + ctx.getRequestContentLanguage()), "");
	}

	public boolean isResized(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		return StringHelper.isTrue(content.getAttribute(ctx, getKey("resized"), null));
	}

	public void setResized(ContentContext ctx, boolean resized) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		content.setAttribute(ctx, getKey("resized"), "" + resized);
	}

	public String getDescription(ContentContext ctx) {
		if (getManualDescription(ctx).length() > 0) {
			return getManualDescription(ctx);
		} else {
			return getLinkedDescription(ctx);
		}
	}

	public void save(ContentContext ctx) throws ServiceException, Exception {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		PersistenceService.getInstance(globalContext).setAskStore(true);
	}

	public void setDescription(ContentContext ctx, String description) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		if (!StringHelper.isEmpty(description)) {
			content.setAttribute(ctx, getKey("description-" + ctx.getRequestContentLanguage()), description);
		} else {
			content.removeAttribute(ctx, getKey("description-" + ctx.getRequestContentLanguage()));
		}
	}

	public String getManualLocation(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		return content.getAttribute(ctx, getKey("location-" + ctx.getRequestContentLanguage()), "");
		// return properties.getString("location-" +
		// ctx.getRequestContentLanguage(), "");
	}

	public String getLocation(ContentContext ctx) {
		if (getManualLocation(ctx).length() > 0) {
			return getManualLocation(ctx);
		} else {
			return getLinkedLocation(ctx);
		}
	}

	public Position getPosition(ContentContext ctx) {
		try {
			return ExifHelper.readPosition(getFile());
		} catch (ImageReadException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*
		 * System.out.println("***** StaticInfo.getPosition : name = "+file.
		 * getName ()); //TODO: remove debug trace
		 * 
		 * Metadata md = getImageMetadata(); if (md != null &&
		 * md.getDirectoriesOfType(GpsDirectory.class) != null) {
		 * Iterator<GpsDirectory> directories =
		 * md.getDirectoriesOfType(GpsDirectory.class).iterator(); if
		 * (directories.hasNext()) { GpsDirectory gpsDirectory = (GpsDirectory)
		 * directories.next(); GeoLocation geoLocation =
		 * gpsDirectory.getGeoLocation(); if (geoLocation != null) {
		 * System.out.println("***** StaticInfo.getPosition : OK"); //TODO:
		 * remove debug trace return new Position(geoLocation.getLatitude(),
		 * geoLocation.getLongitude()); } else {
		 * System.out.println("***** StaticInfo.getPosition : no geo"); //TODO:
		 * remove debug trace } }
		 * System.out.println("***** StaticInfo.getPosition : NO NEXT"); //TODO:
		 * remove debug trace } else {
		 * System.out.println("***** StaticInfo.getPosition : md="+md); //TODO:
		 * remove debug trace
		 * System.out.println("***** StaticInfo.getPosition : NULL"); //TODO:
		 * remove debug trace }
		 */
		return null;
	}

	public String getGeoLocation(ContentContext ctx) throws IOException {
		Position pos = getPosition(ctx);
		if (pos != null) {
			return LocationService.getLocation(pos.getLatitude(), pos.getLongitude(), ctx.getLanguage()).getFullLocality();
		} else {
			return null;
		}
	}

	public void setLocation(ContentContext ctx, String location) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		if (StringHelper.isEmpty(location)) {
			content.removeAttribute(ctx, getKey("location-" + ctx.getRequestContentLanguage()));
		} else {
			content.setAttribute(ctx, getKey("location-" + ctx.getRequestContentLanguage()), location);
		}
	}

	public String getTitle(ContentContext ctx) {
		if (getManualTitle(ctx).length() > 0) {
			return getManualTitle(ctx);
		}
		return "";
	}

	public String getManualTitle(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		String key = getKey("title-" + ctx.getRequestContentLanguage());
		String title = content.getAttribute(ctx, key, "");
		return title;
	}

	public String getId(ContentContext ctx) {
		if (id == null) {
			ContentService content = ContentService.getInstance(ctx.getGlobalContext());
			String key = getKey("id-" + ctx.getRequestContentLanguage());
			id = content.getAttribute(ctx, key, "");
			if (id.trim().length() == 0) {
				id = StringHelper.getRandomId();
				content.setAttribute(ctx, key, id);
			}
		}
		return id;
	}

	public void setTitle(ContentContext ctx, String title) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		if (StringHelper.isEmpty(title)) {
			content.removeAttribute(ctx, getKey("title-" + ctx.getRequestContentLanguage()));
		} else {
			content.setAttribute(ctx, getKey("title-" + ctx.getRequestContentLanguage()), title);
		}
	}

	public Date getManualDate(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		String dateStr = content.getAttribute(ctx, getKey("date"), null);
		if (dateStr != null) {
			try {
				return StringHelper.parseTime(dateStr);
			} catch (ParseException e) {
			}
		}
		return null;
	}

	public Date getCreationDate(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		String dateStr = content.getAttribute(ctx, getKey("creation-date"), null);
		if (dateStr == null) {
			dateStr = StringHelper.renderTime(new Date());
			content.setAttribute(ctx, getKey("creation-date"), dateStr);
			try {
				PersistenceService.getInstance(ctx.getGlobalContext()).setAskStore(true);
			} catch (ServiceException e) {
				e.printStackTrace();
				return null;
			}
		}
		try {
			return StringHelper.parseTime(dateStr);
		} catch (ParseException e) {
		}
		return null;
	}

	public String getAuthors(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		return content.getAttribute(ctx, getKey("authors"), "");
	}

	public void setAuthors(ContentContext ctx, String authors) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		if (StringHelper.isEmpty(authors)) {
			content.removeAttribute(ctx, getKey("authors"));
		} else {
			content.setAttribute(ctx, getKey("authors"), authors);
		}
	}

	public Date getDate(ContentContext ctx) {
		if (date == null) {
			if (getManualDate(ctx) != null) {
				date = getManualDate(ctx);
			} else {
				if (getExifDate() != null) {
					date = getExifDate();
				} else {
					Date linkedDate = getLinkedDate(ctx);
					if (linkedDate == null) {
						dateFromData = false;
						date = getFileDate(ctx);
					} else {
						date = linkedDate;
					}
				}
			}
		}
		return date;
	}

	public Date getFileDate(ContentContext ctx) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		ICache fileInfoCache = globalContext.getEternalCache("file-info");
		Date outDate = (Date) fileInfoCache.get(getKey("file-date"));
		if (outDate == null) {
			outDate = getFileDate();
			fileInfoCache.put(getKey("file-date"), outDate);
		}
		return outDate;
	}

	public void setDate(ContentContext ctx, Date date) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		if (date == null) {
			content.removeAttribute(ctx, getKey("date"));
		} else {
			content.setAttribute(ctx, getKey("date"), StringHelper.renderTime(date));
		}
	}

	public String getResource(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		return content.getAttribute(ctx, getKey("resource"), "");
	}

	public void setResource(ContentContext ctx, String resource) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		content.setAttribute(ctx, getKey("resource"), resource);
	}

	public void setEmptyDate(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		content.setAttribute(ctx, getKey("date"), "");
	}

	public boolean isEmptyDate(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		return content.getAttribute(ctx, getKey("date"), null) == null;
	}

	public long getSize() {
		return size;
	}

	public String getLinkedPageId(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		return content.getAttribute(ctx, getKey("linked-page-id"));
	}

	public void setLinkedPageId(ContentContext ctx, String pageId) {
		if (pageId != null) {
			ContentService content = ContentService.getInstance(ctx.getGlobalContext());
			content.setAttribute(ctx, getKey("linked-page-id"), pageId);
		}
	}

	public String getStaticURL() {
		return staticURL;
	}

	public String getFolder() {
		return StringUtils.chomp(ResourceHelper.getPath(getStaticURL()), "/");
	}

	/**
	 * return the focus point of a image. Return always the focus point of edit
	 * mode (problem with image cache).
	 * 
	 * @param ctx
	 * @return
	 */
	public int getFocusZoneX(ContentContext ctx) {
		
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());

		ContentContext editCtx = ctx.getContextWithOtherRenderMode(ContentContext.EDIT_MODE);
		if (!content.isNavigationLoaded(editCtx)) {
			editCtx = ctx;
		}
		if (content.getAttribute(editCtx, getKey("focus-zone-x"), null) == null) {
			if (StringHelper.isImage(getFile().getName())) {
				try {
					if (getFile().exists()) {
						Point point = null;
						BufferedImage img = null;
						try {
							img = ImageIO.read(getFile());
							if (ctx.getGlobalContext().getStaticConfig().isAutoFocus()) {
								point = InitInterest.getPointOfInterest(img);
							}
						} catch (Throwable t) {
							logger.warning(t.getMessage());
						}
						if (point != null && img != null) {
							int focusX = (point.getX() * 1000) / img.getWidth();
							int focusY = (point.getY() * 1000) / img.getHeight();

							content.setAttribute(editCtx, getKey("focus-zone-x"), "" + focusX);
							content.setAttribute(editCtx, getKey("focus-zone-y"), "" + focusY);
						} else {
							content.setAttribute(editCtx, getKey("focus-zone-x"), "" + DEFAULT_FOCUS_X);
							content.setAttribute(editCtx, getKey("focus-zone-y"), "" + DEFAULT_FOCUS_Y);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		String kzx = content.getAttribute(editCtx, getKey("focus-zone-x"), "" + DEFAULT_FOCUS_X);
		return Integer.parseInt(kzx);
	}

	/**
	 * return the focus point of a image. Return always the focus point of edit
	 * mode (problem with image cache).
	 * 
	 * @param ctx
	 * @return
	 */
	public int getFocusZoneY(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		ContentContext editCtx = ctx.getContextWithOtherRenderMode(ContentContext.EDIT_MODE);
		if (!content.isNavigationLoaded(editCtx)) {
			editCtx = ctx;
		}
		if (content.getAttribute(editCtx, getKey("focus-zone-x"), null) == null) {
			getFocusZoneX(editCtx); // generate default value
		}
		String kzy = content.getAttribute(editCtx, getKey("focus-zone-y"), "" + DEFAULT_FOCUS_Y);
		return Integer.parseInt(kzy);
	}

	public boolean isShared(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		return StringHelper.isTrue(content.getAttribute(ctx, getKey("shared"), "true"));
	}

	public void setShared(ContentContext ctx, boolean shared) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		if (shared) {
			content.removeAttribute(ctx, getKey("shared"));
		} else {
			content.setAttribute(ctx, getKey("shared"), "" + shared);
		}
	}

	/**
	 * change the focus point on a image.
	 * 
	 * @param ctx
	 * @param focusZoneX
	 */
	public void setFocusZoneX(ContentContext ctx, int focusZoneX) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		if (focusZoneX == DEFAULT_FOCUS_X) {
			content.removeAttribute(ctx, getKey("focus-zone-x"));
		} else {
			content.setAttribute(ctx, getKey("focus-zone-x"), "" + focusZoneX);
		}
	}

	/**
	 * change the focus point on a image.
	 * 
	 * @param ctx
	 * @param focusZoneY
	 */
	public void setFocusZoneY(ContentContext ctx, int focusZoneY) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		if (focusZoneY == DEFAULT_FOCUS_Y) {
			content.removeAttribute(ctx, getKey("focus-zone-y"));
		} else {
			content.setAttribute(ctx, getKey("focus-zone-y"), "" + focusZoneY);
		}
	}

	public MenuElement getLinkedPage(ContentContext ctx) {
		if (getLinkedPageId(ctx) != null && linkedPage == null) {
			try {
				GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
				NavigationService navigationService = NavigationService.getInstance(globalContext);
				linkedPage = navigationService.getPage(ctx, getLinkedPageId(ctx));
				// linkedPage =
				// content.getNavigation(ctx).searchChildFromId(getLinkedPageId(ctx));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return linkedPage;
	}

	public Date getLinkedDate(ContentContext ctx) {
		if (getLinkedPage(ctx) != null) {
			if (linkedDate == null) {
				try {
					linkedDate = getLinkedPage(ctx).getContentDate(ctx);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return linkedDate;
		} else {
			return null;
		}
	}

	private Date getFileDate() {
		if (fileDate == null) {
			fileDate = new Date(getFile().lastModified());
		}
		return fileDate;
	}

	public void setLinkedDate(Date linkedDate) {
		this.linkedDate = linkedDate;
	}

	public String getLinkedTitle(ContentContext ctx) {
		if (getLinkedPage(ctx) != null) {
			if (linkedTitle == null) {
				try {
					linkedTitle = getLinkedPage(ctx).getTitle(ctx);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return linkedTitle;
		} else {
			return "";
		}
	}

	public void setLinkedTitle(String linkedTitle) {
		this.linkedTitle = linkedTitle;
	}

	public String getLinkedDescription(ContentContext ctx) {
		if (getLinkedPage(ctx) != null) {
			if (linkedDescription == null) {
				try {
					linkedDescription = getLinkedPage(ctx).getDescription(ctx);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return linkedDescription;
		} else {
			return "";
		}
	}

	public void setLinkedDescription(String linkedDescription) {
		this.linkedDescription = linkedDescription;
	}

	public void setLinkedLocation(String linkedLocation) {
		this.linkedLocation = linkedLocation;
	}

	public String getLinkedLocation(ContentContext ctx) {
		if (getLinkedPage(ctx) != null) {
			if (linkedLocation == null) {
				try {
					linkedLocation = getLinkedPage(ctx).getLocation(ctx);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return linkedLocation;
		} else {
			return "";
		}
	}

	public boolean isPertinent(ContentContext ctx) {
		boolean outPertinent = getManualTitle(ctx).length() > 0 || getManualDescription(ctx).length() > 0 || getManualLocation(ctx).length() > 0;
		return outPertinent;

	}

	public boolean delete(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		content.deleteKeys(getKey(""));
		return true;
	}

	public void setFile(File file) {
		crc32 = null;
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	public long getCRC32() {
		if (crc32 == null) {
			try {
				if (file.isFile()) {
					crc32 = FileUtils.checksumCRC32(file);
				} else {
					crc32 = 0L;
				}
			} catch (IOException e) {
				e.printStackTrace();
				crc32 = 0L;
			}
		}
		return crc32;
	}

	public void renameFile(ContentContext ctx, File newFile) throws Exception {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		StaticInfo newStaticInfo = StaticInfo.getInstance(ctx, newFile);
		content.renameKeys(getKey(""), newStaticInfo.getKey(""));
	}

	public void deleteFile(ContentContext ctx) throws Exception {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		content.deleteKeys(getKey(""));
	}

	public void duplicateFile(ContentContext ctx, File newFile) throws Exception {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		StaticInfo newStaticInfo = StaticInfo.getInstance(ctx, newFile);
		content.duplicateKeys(getKey(""), newStaticInfo.getKey(""));
	}

	public int getAccessFromSomeDays(ContentContext ctx) throws Exception {
		if (accessFromSomeDays < 0) {
			StaticConfig staticConfig = StaticConfig.getInstance(ctx.getRequest().getSession());
			Calendar cal = Calendar.getInstance();
			int countDay = 0;
			int outAccess = 0;
			while (countDay < staticConfig.getLastAccessStatic()) {
				outAccess = outAccess + getAccess(ctx, cal.getTime());
				cal.roll(Calendar.DAY_OF_YEAR, false);
				countDay++;
			}
			accessFromSomeDays = outAccess;
		}
		return accessFromSomeDays;
	}

	private static String getAccessKey(Date date) {
		return "access-" + StringHelper.renderDate(date, GlobalContext.ACCESS_DATE_FORMAT);
	}

	public int getAccess(ContentContext ctx, Date date) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		String key = getKey(getAccessKey(date));
		if (globalContext.getData(key) == null) {
			return 0;
		}
		return Integer.parseInt(globalContext.getData(key));
	}

	public void addAccess(ContentContext ctx) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		if (globalContext.getSpecialConfig().isTrackingAccess()) {
			String key = getKey(getAccessKey(new Date()));
			int todayAccess = 0;
			if (globalContext.getData(key) != null) {
				todayAccess = Integer.parseInt(globalContext.getData(key));
			}
			todayAccess++;
			globalContext.setData(key, "" + todayAccess);
		}
	}

	private void storeTags(ContentContext ctx) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		String key = getKey("tags");
		String rawTags = StringHelper.collectionToString(tags);
		globalContext.setData(key, rawTags);
	}

	public List<String> getTags(ContentContext ctx) {
		if (tags == null) {
			String key = getKey("tags");
			GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
			String rawTags = globalContext.getData(key);
			if (rawTags == null) {
				tags = Collections.EMPTY_LIST;
			} else {
				tags = StringHelper.stringToCollection(rawTags);
			}
		}
		return tags;
	}

	public void addTag(ContentContext ctx, String tag) {
		if (tags == null || tags == Collections.EMPTY_LIST) {
			tags = new LinkedList<String>();
		}
		if (!tags.contains(tag)) {
			tags.add(tag);
			storeTags(ctx);
		}
	}

	public void removeTag(ContentContext ctx, String tag) {
		if (tags == null || tags == Collections.EMPTY_LIST) {
			tags = new LinkedList<String>();
		}
		if (tags.contains(tag)) {
			tags.remove(tag);
			storeTags(ctx);
		}
	}

	private void storeReadRoles(ContentContext ctx) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		String key = getKey("read-roles");
		String rawRoles = StringHelper.collectionToString(readRoles);
		globalContext.setData(key, rawRoles);
	}

	public List<String> getReadRoles(ContentContext ctx) {
		if (readRoles == null) {
			String key = getKey("read-roles");
			GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
			String rawReadRole = globalContext.getData(key);
			if (rawReadRole == null) {
				readRoles = Collections.EMPTY_LIST;
			} else {
				readRoles = StringHelper.stringToCollection(rawReadRole);
			}
		}
		return readRoles;
	}

	public void addReadRole(ContentContext ctx, String role) {
		if (readRoles == null || readRoles == Collections.EMPTY_LIST) {
			readRoles = new LinkedList<String>();
		}
		if (!readRoles.contains(role)) {
			readRoles.add(role);
			storeReadRoles(ctx);
		}
	}

	public void removeReadRole(ContentContext ctx, String role) {
		readRoles = getReadRoles(ctx);
		if (readRoles.contains(role)) {
			readRoles.remove(role);
			storeReadRoles(ctx);
		}
	}

	public StaticInfo getDirecotry(ContentContext ctx) throws Exception {
		if (getFile().exists()) {
			return StaticInfo.getInstance(ctx, getFile().getParentFile());
		} else {
			return null;
		}
	}

	public String getVersionHash(ContentContext ctx) {
		return StringHelper.asBase64(getFocusZoneX(ctx) * getFocusZoneY(ctx)) + StringHelper.asBase64(getCRC32());
	}

	public Date getExifDate() {
		try {
			return ExifHelper.readDate(getFile());
		} catch (ImageReadException e) {
			logger.warning(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*
		 * Metadata md = getImageMetadata(); if (md != null) { // obtain the
		 * Exif directory ExifSubIFDDirectory directory =
		 * md.getFirstDirectoryOfType(ExifSubIFDDirectory.class); // query the
		 * tag's value if (directory != null) { return
		 * directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL); } }
		 */
		return null;
	}

	public String getCopyright(ContentContext ctx) {
		ContentService content = ContentService.getInstance(ctx.getGlobalContext());
		return content.getAttribute(ctx, getKey("copyright"), "");
	}

	public void setCopyright(ContentContext ctx, String copyright) {
		if (copyright != null) {
			ContentService content = ContentService.getInstance(ctx.getGlobalContext());
			content.setAttribute(ctx, getKey("copyright"), copyright);
		}
	}

	public String getFullHTMLTitle(ContentContext ctx) throws Exception {
		String title = getTitle(ctx);
		StaticInfo folder = StaticInfo.getInstance(ctx, file.getParentFile());
		if (title == null || title.trim().length() > 0) {
			title = folder.getTitle(ctx);
		}
		String location = getLocation(ctx);
		if (location == null || location.trim().length() > 0) {
			location = folder.getLocation(ctx);
		}
		String date = null;
		try {
			if (dateFromData) {
				date = StringHelper.renderShortDate(ctx, getDate(ctx));
			} else if (folder.dateFromData) {
				date = StringHelper.renderShortDate(ctx, folder.getDate(ctx));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String sep = "";

		if (title != null && title.trim().length() > 0) {
			title = "<span class=\"title\">" + title + "</span>";
			sep = " - ";
		}

		if (location != null && location.trim().length() > 0) {
			title = title + "<span class=\"location\">" + sep + location + "</span>";
			sep = " - ";
		}
		if (date != null) {
			title = title + "<span class=\"date\">" + sep + date + "</span>";
		}
		return title;
	}

	public String getFullTitle(ContentContext ctx) throws Exception {
		String title = getTitle(ctx);
		StaticInfo folder = StaticInfo.getInstance(ctx, file.getParentFile());
		if (title == null || title.trim().length() == 0) {
			title = folder.getTitle(ctx);
		}
		String location = getLocation(ctx);
		if (location == null || location.trim().length() == 0) {
			location = folder.getLocation(ctx);
		}
		String date = null;
		try {
			if (dateFromData) {
				date = StringHelper.renderShortDate(ctx, getDate(ctx));
			} else if (folder.dateFromData) {
				date = StringHelper.renderShortDate(ctx, folder.getDate(ctx));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String sep = "";

		if (title != null && title.trim().length() > 0) {
			sep = " - ";
		}

		if (location != null && location.trim().length() > 0) {
			title = title + sep + location;
			sep = " - ";
		}
		if (date != null) {
			title = title + sep + date;
		}
		return title;
	}

	public Boolean canRead(ContentContext ctx, User user, String accessToken) {
		if (accessToken != null && accessToken.equals(getAccessToken(ctx))) {
			return true;
		}
		List<String> roles = getReadRoles(ctx);
		if (roles == null || roles.size() == 0) {
			return true;
		}
		if (user == null) {
			return false;
		} else {
			return !Collections.disjoint(roles, user.getUserInfo().getRoles());
		}
	}

	public void setAccessToken(ContentContext ctx, String token) {
		ctx.getGlobalContext().setTimeAttribute(getKey("accesstoken"), token, 120);
	}

	public String getAccessToken(ContentContext ctx) {
		String key = getKey("accesstoken");
		String token = (String) ctx.getGlobalContext().getTimeAttribute(key);
		if (token == null) {
			token = StringHelper.getRandomString(32, StringHelper.ALPHANUM);
			setAccessToken(ctx, token);
		}
		return token;
	}

	public String getURL(ContentContext ctx) {
		return URLHelper.createResourceURL(ctx, getFile());
	}

	public boolean isStaticFolder() {
		return staticFolder;
	}

	public void setStaticFolder(boolean staticFolder) {
		this.staticFolder = staticFolder;
	}

}
