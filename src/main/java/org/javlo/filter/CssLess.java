package org.javlo.filter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.javlo.config.StaticConfig;
import org.javlo.context.ContentContext;
import org.javlo.context.GlobalContext;
import org.javlo.helper.ResourceHelper;
import org.lesscss.LessCompiler;

public class CssLess implements Filter {
	
	private static Logger logger = Logger.getLogger(CssLess.class.getName());

	 @Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain next) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String path = httpRequest.getServletPath();
		GlobalContext globalContext = GlobalContext.getInstance(httpRequest);
		if (path.startsWith('/' + globalContext.getContextKey())) {
			path = path.replaceFirst('/' + globalContext.getContextKey(), "");
		}		
		File cssFile = new File(httpRequest.getSession().getServletContext().getRealPath(path));
		boolean compileLess = !cssFile.exists();
		File lessFile = new File(cssFile.getAbsolutePath().substring(0, cssFile.getAbsolutePath().length() - 4) + ".less");
		if (!compileLess) {
			if (!StaticConfig.getInstance(((HttpServletRequest) request).getSession().getServletContext()).isProd()) {
				if (lessFile.lastModified() > cssFile.lastModified()) {
					compileLess = true;
				}
			}
		}
		if (compileLess) {
			if (!globalContext.getContextKey().equals(globalContext.getMainContextKey())) {
				lessFile = new File(StringUtils.replaceOnce(lessFile.getAbsolutePath(), File.separator+globalContext.getMainContextKey()+File.separator, File.separator+globalContext.getContextKey()+File.separator));
				cssFile.getParentFile().mkdirs();
			}
			if (lessFile.exists()) {
				if (compile (lessFile, cssFile, globalContext.getStaticConfig().isProd())) {					
					try {
						Thread.sleep(5*1000); // check why on linux we need the sleep.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		next.doFilter(request, response);
	}
	
	private static boolean compile(File lessFile, File cssFile, boolean compress) {		
		LessCompiler lessCompiler = new LessCompiler();
		FileOutputStream out = null;
		try {
			lessCompiler.setEncoding(ContentContext.CHARACTER_ENCODING);
			lessCompiler.setCompress(compress);
			String cssContent = lessCompiler.compile(lessFile);
			out = new FileOutputStream(cssFile);
			ResourceHelper.writeStringToStream(cssContent, out, ContentContext.CHARACTER_ENCODING);
			out.flush();
			out.getFD().sync();		
			return true;
		} catch (Exception e) {
			logger.severe("error on less file '"+lessFile+"' : "+e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			ResourceHelper.closeResource(out);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

	@Override
	public void destroy() {
	}

}

