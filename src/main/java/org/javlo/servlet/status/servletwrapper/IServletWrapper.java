package org.javlo.servlet.status.servletwrapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IServletWrapper {
	
	public void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException;

}
