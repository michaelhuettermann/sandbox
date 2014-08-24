package com.huettermann.web;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SiteRemoteIT {

    private WebDriver driver;

    @Before
    public void setUp() {
        driver = new HtmlUnitDriver();
    }

    @Test
    public void testRemote() {
        driver.get("http://www.google.de");
        WebElement element = driver.findElement(By.name("q"));
        element.sendKeys("hallo");
        element.submit();

        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getTitle().toLowerCase().startsWith("hallo");
            }
        });

        Assert.assertEquals("hallo", driver.getTitle().substring(0, 5));

    }

    @After
    public void tearDown() {
        driver.quit();
    }
    
} 
