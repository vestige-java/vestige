//package com.googlecode.vestige.admin.web;
//
//import io.milton.http.HttpManager;
//import io.milton.http.Request;
//import io.milton.http.Response;
//import io.milton.servlet.ServletConfigWrapper;
//import io.milton.servlet.ServletRequest;
//import io.milton.servlet.ServletResponse;
//
//import java.io.IOException;
//
//import javax.servlet.Servlet;
//import javax.servlet.ServletConfig;
//import javax.servlet.ServletContext;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * @author Gael Lalire
// */
//public class VestigeWebdavServlet implements Servlet {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeWebdavServlet.class);
//
//    private ServletConfigWrapper config;
//    private ServletContext servletContext;
//    private HttpManager httpManager;
//
//    public VestigeWebdavServlet(final HttpManager httpManager) {
//        this.httpManager = httpManager;
//    }
//
//    @Override
//    public void init(final ServletConfig config) throws ServletException {
//        try {
//            this.config = new ServletConfigWrapper(config);
//            this.servletContext = config.getServletContext();
//        } catch (Throwable ex) {
//            LOGGER.error("Exception starting milton servlet", ex);
//            throw new RuntimeException(ex);
//        }
//    }
//
//    @Override
//    public void destroy() {
//    }
//
//    @Override
//    public void service(final javax.servlet.ServletRequest servletRequest, final javax.servlet.ServletResponse servletResponse) throws ServletException, IOException {
//        HttpServletRequest req = (HttpServletRequest) servletRequest;
//        HttpServletResponse resp = (HttpServletResponse) servletResponse;
//        try {
//            Request request = new ServletRequest(req, servletContext);
//            Response response = new ServletResponse(resp);
//            httpManager.process(request, response);
//        } finally {
//            servletResponse.getOutputStream().flush();
//            servletResponse.flushBuffer();
//        }
//    }
//
//    @Override
//    public String getServletInfo() {
//        return "MiltonServlet";
//    }
//
//    @Override
//    public ServletConfig getServletConfig() {
//        return config.getServletConfig();
//    }
//
//}
