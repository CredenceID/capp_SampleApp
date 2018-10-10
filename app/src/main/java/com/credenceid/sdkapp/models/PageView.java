package com.credenceid.sdkapp.models;

import com.credenceid.biometrics.Biometrics;

/* Throughout this application a user may switch between the various pages many times. Each page may
 * be thought of as an Activity. So a user switches from Activity to Activity. Since our "pages" do
 * not extend th Activity class, we need someway for these pages to mimic certain Activity lifecycle
 * events, specifically the important ones. Such as "onCreate()", "onDestroy()", and so forth. To
 * achieve this all pages extends this class and implement the following methods.
 *
 */
public interface PageView {
    // Returns current pages title.
    String getTitle();

    /* Every time we switch to a page we "activate" it. You can sort of think of this similar to an
     * Activity's "onCreate()" method.
     */
    void
    activate(Biometrics biometrics);

    /* Every time we switch out of a page or an Android home/menu/back button is pressed we need to
     * be able to properly "onDestroy()/onPause()" the page, just like an Activity.
     */
    void
    deactivate();

    /* Lastly, just like any other Activity if it is interrupted by say a "Home" button it would
     * goto an "onPause()" and then when we select to an "onResume()". Each page will implement a
     * similarly named method in order to mimic this "onResume()" functionality.
     */
    void
    doResume();
}
