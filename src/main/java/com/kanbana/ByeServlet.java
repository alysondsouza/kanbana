package com.kanbana;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * ByeServlet — handles GET /bye
 * Extends HttpServlet to plug into Tomcat's request lifecycle.
 */
public class ByeServlet extends HttpServlet {

    /**
     * Called by Tomcat when a GET request arrives at /bye
     *
     * @param request  — contains everything about the incoming HTTP request
     * @param response — the object you write your response into
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Tell the browser this is plain text, encoded in UTF-8
        response.setContentType("text/plain;charset=UTF-8");

        // PrintWriter lets you write text into the HTTP response body
        PrintWriter out = response.getWriter();
        out.println("Ciao!");
    }
}
