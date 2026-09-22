package com.dlis.web.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application — registers all REST resources under /api/v1
 */
@ApplicationPath("/api/v1")
public class DlisApplication extends Application {
    // Resources are auto-discovered via CDI
}
