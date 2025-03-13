package com.example.agent;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class CustomResponseWrapper extends HttpServletResponseWrapper {
    public CustomResponseWrapper(HttpServletResponse response) {
        super(response);
        System.out.println("✅ CustomResponseWrapper applied!");
    }
}
