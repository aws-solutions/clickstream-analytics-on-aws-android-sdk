# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import pytest
from time import sleep

from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.common.appiumby import AppiumBy
from selenium.common.exceptions import NoSuchElementException

capabilities = dict(
    platformName='Android',
    automationName='uiautomator2',
    deviceName='Android',
    appPackage='com.kanyideveloper.joomia',
    appActivity='.core.presentation.MainActivity',
    language='en',
    locale='US',
)

appium_server_url = 'http://0.0.0.0:4723/wd/hub'


class TestShopping:
    def setup(self):
        self.driver = webdriver.Remote(appium_server_url, options=UiAutomator2Options().load_capabilities(capabilities))
        self.driver.implicitly_wait(10)

    def teardown(self):
        if self.driver:
            self.driver.quit()

    @pytest.mark.parametrize("user_name,password", [
        ("user1", "password1"),
        ("user2", "password2"),
    ])
    def test_shopping(self, user_name, password):
        # login
        username_et = self.find_element("userName")
        username_et.send_keys(user_name)
        password_et = self.find_element("password")
        password_et.send_keys(password)
        signin_bt = self.find_element("signIn")
        signin_bt.click()
        sleep(3)

        # add 2 products to cart
        product_1 = self.find_element('product0')
        product_1.click()
        sleep(2)
        add_to_cart1 = self.find_element('add_to_cart_button')
        add_to_cart1.click()
        sleep(2)
        self.driver.press_keycode(4)
        sleep(2)
        product_2 = self.find_element('product1')
        product_2.click()
        sleep(2)
        add_to_cart2 = self.find_element('add_to_cart_button')
        add_to_cart2.click()
        sleep(2)
        self.driver.press_keycode(4)
        sleep(2)

        # add 1 product to wishlist
        product_3 = self.find_element('product2')
        product_3.click()
        sleep(2)
        like_button = self.find_element('like_button')
        like_button.click()
        sleep(2)
        self.driver.press_keycode(4)
        sleep(2)
        wishlist_tab = self.find_element('homeTab1')
        wishlist_tab.click()
        sleep(2)

        cart_tab = self.find_element('homeTab2')
        cart_tab.click()
        sleep(2)
        checkout_bt = self.find_element('check_out_button')
        checkout_bt.click()
        sleep(2)

        profile_tab = self.find_element('homeTab3')
        profile_tab.click()
        sleep(2)
        sign_out_bt = self.find_element('sign_out_button')
        sign_out_bt.click()
        sleep(2)
        self.driver.press_keycode(3)
        sleep(5)

    def find_element(self, name):
        try:
            return self.driver.find_element(AppiumBy.ANDROID_UIAUTOMATOR,
                                            value='new UiSelector().resourceId("' + name + '")')
        except NoSuchElementException:
            pytest.skip(f"Element with name: '{name}' not found. Skipped the test")


if __name__ == '__main__':
    TestShopping.test_shopping()
