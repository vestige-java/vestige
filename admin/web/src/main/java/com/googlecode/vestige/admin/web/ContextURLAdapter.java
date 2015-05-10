//package com.googlecode.vestige.admin.web;
//
//import io.milton.http.HttpManager;
//import io.milton.http.Request;
//import io.milton.http.UrlAdapter;
//
///**
// * @author Gael Lalire
// */
//public class ContextURLAdapter implements UrlAdapter {
//
//    private String context;
//
//    public ContextURLAdapter(final String context) {
//        this.context = context;
//    }
//
//    @Override
//    public String getUrl(final Request request) {
//        String s = HttpManager.decodeUrl(request.getAbsolutePath());
//        if (s.contains("/" + context)) {
//            return s.replace("/" + context, "");
//        } else {
//            return s;
//        }
//    }
//
//}
