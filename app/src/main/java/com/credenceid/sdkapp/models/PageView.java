package com.credenceid.sdkapp.models;

import com.credenceid.biometrics.Biometrics;

/* Custom interface for each page used inside our application. When working with CredenceService
 * or SDK we need to be aware of how to properly handle its object and be aware of its ability to
 * bing/unbind. In order to handle these we define a custom interface where inside these methods
 * we can choose what to do with our Service object.
 *
 * This will make more sense as you see this in use throughout the different pages.
 */
public interface PageView {
    /* Returns current pages title. */
    String getTitle();

    /* Every time we switch to a page we "activate" it. You can sort of think of this similar to an
     * Activity's "onCreate()" method.
     */
    void activate(Biometrics biometrics);

    /* Every time we switch out of a page or an Android home/menu/back button is pressed we need to
     * be able to properly "onDestroy()/onPause()" the page, just like an Activity.
     */
    void deactivate();

    /* Lastly, just like any other Activity if it is interrupted by say a "home" button it would
     * goto an "onPause()" and then when we select to an "onResume()". To mimic that we also have
     * a method for just that!
     */
    void doResume();
}
