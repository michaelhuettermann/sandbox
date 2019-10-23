package com.huettermann.all;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Message extends HttpServlet {

    private static Logger logger = LoggerFactory.getLogger(Message.class);

    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException {

        String param = request.getParameter("param");
        ServletContext context = getServletContext();

        if (param == null || param.equals("")) {
            context.log("No message received:",
                    new IllegalStateException("Missing parameter"));
        } else {
            context.log("Here the paramater: " + param);
        }

        PrintWriter out = null;
        //if (request.getRequestedSessionId().equals("4711")) { //NOSONAR
            try {
                out = response.getWriter();
                String encodedName = org.owasp.encoder.Encode.forHtml(param);
                out.println("Hello: " + encodedName);
                out.flush();
                out.close();
            } catch (IOException io) {
                logger.info(io.getMessage());
            }
        //} //NOSONAR
    }
}