package com.huettermann.all;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiteLocalIT {

    static final Logger logger =
            LoggerFactory.getLogger(SiteLocalIT.class);

    private WebDriver driver;

    @Before
    public void setUp() {
        driver = new HtmlUnitDriver();
    }

    @Test
    public void testLocal() {
        driver.get("http://localhost:8001/all");
        logger.info("Let's check the title.");
        assertTrue(driver.getTitle().contains("Welcome in the Cloud!"));
    }

    @After
    public void tearDown() {
        driver.quit();
    }
    
} 
