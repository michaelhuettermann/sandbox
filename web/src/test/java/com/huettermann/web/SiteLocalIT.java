package com.huettermann.web;

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

public class SiteLocalIT {

    private WebDriver driver;

    @Before
    public void setUp() {
        driver = new HtmlUnitDriver();
    }

    @Test
    public void testLocal() {
        driver.get("http://localhost:8080/web");
        WebElement element = driver.findElement(By.xpath("//h2[1]"));
        Assert.assertEquals("Hello World!", element.getText());
    }

    @After
    public void tearDown() {
        driver.quit();
    }
    
} 
