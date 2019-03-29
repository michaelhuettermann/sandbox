package com.huettermann.all;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Message extends HttpServlet {

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
        try {
            out = response.getWriter();
            out.println("Hello: " + param);
            out.flush();
            out.close();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}