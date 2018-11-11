package com.huettermann.all;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.crypto.NullCipher;

public class Message extends HttpServlet {
    
    //private String userName;  // As this field is shared by all users, it's obvious that this piece of information should be managed differently

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws ServletException, IOException {

        String param = request.getParameter("param");
        ServletContext context = getServletContext( );

        if (param == null || param.equals("")) {
	        context.log("No message received:",
            new IllegalStateException("Missing parameter"));
        } else {
            context.log("Here the paramater: " + param);
        }

        if(request.getRequestedSessionId() ) {
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
} 
